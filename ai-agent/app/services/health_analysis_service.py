"""
健康分析服务
用于生成结构化的健康分析结果
"""
import json
import re
import logging
from typing import Dict, Any, Optional, List

from app.db.health_data_client import HealthDataClient
from app.core.data_processor import DataProcessor
from app.core.medical_content_builder import build_medical_contents
from app.core.medical_prompts import HEALTH_ANALYSIS_PROMPT_V1
from app.core.multimodal_builder import build_medical_messages
from app.core.llm_client import LlmClient
from app.core.health_record_storage import HealthRecordStorage
from app.core.extended_data_schema import (
    build_default_extended_data,
    merge_extended_data,
    validate_extended_data
)
from app.core.pipeline_trace import (
    log_analysis_input_sizes,
    log_archive_json_summary,
    log_medical_contents,
    log_raw_incoming,
)

# 配置日志
logger = logging.getLogger(__name__)

# 固定的记录ID，用于读取健康档案
_ARCHIVE_RECORD_ID = "latest"

_BACKEND_RISK_LEVELS = frozenset({"低", "中", "高", "极高"})

_KNOWN_DIMENSION_KEYS = frozenset(
    {
        "dimensionCode",
        "dimensionName",
        "riskLevel",
        "evidence",
        "interpretation",
        "confidence",
    }
)


def _coerce_evidence_list(ev: Any) -> List[Dict[str, str]]:
    if ev is None:
        return []
    if isinstance(ev, str):
        t = ev.strip()
        if not t:
            return []
        return [{"indicator": "说明", "value": t[:800], "reference": ""}]
    if isinstance(ev, dict):
        rows: List[Dict[str, str]] = []
        for ik, iv in ev.items():
            rows.append(
                {
                    "indicator": str(ik),
                    "value": "" if iv is None else str(iv),
                    "reference": "",
                }
            )
        return rows
    if not isinstance(ev, list):
        return []
    out: List[Dict[str, str]] = []
    for e in ev:
        if not isinstance(e, dict):
            continue
        if "indicator" in e or "value" in e or "reference" in e:
            out.append(
                {
                    "indicator": str(e.get("indicator", "")),
                    "value": "" if e.get("value") is None else str(e.get("value")),
                    "reference": str(e.get("reference", "")),
                }
            )
        else:
            for ik, iv in e.items():
                out.append(
                    {
                        "indicator": str(ik),
                        "value": "" if iv is None else str(iv),
                        "reference": "",
                    }
                )
    return out


def _dimension_analysis_rows_from_map(d: Dict[str, Any]) -> List[Dict[str, Any]]:
    """将 { \"维度名\": 分值, ... } 转为后端 DimensionAnalysis 数组。"""
    rows: List[Dict[str, Any]] = []
    for k, v in d.items():
        if not isinstance(k, str):
            continue
        name = k.strip()
        if not name:
            continue
        val_str = "" if v is None else str(v)
        rows.append(
            {
                "dimensionCode": "",
                "dimensionName": name,
                "riskLevel": "中",
                "evidence": [
                    {"indicator": name, "value": val_str, "reference": ""}
                ],
                "interpretation": "",
                "confidence": 0.75,
            }
        )
    return rows


def _normalize_one_dimension_item(it: Dict[str, Any]) -> Optional[Dict[str, Any]]:
    """将单条维度分析规整为 Java DTO 形状。"""
    if _KNOWN_DIMENSION_KEYS.intersection(it.keys()):
        row = dict(it)
        row.setdefault("dimensionCode", "")
        row.setdefault("dimensionName", str(row.get("dimensionName") or ""))
        row.setdefault("riskLevel", "中")
        row.setdefault("interpretation", "")
        conf = row.get("confidence")
        if conf is None:
            row["confidence"] = 0.75
        row["evidence"] = _coerce_evidence_list(row.get("evidence"))
        return row
    return None


def _normalize_dimension_analysis_for_api(val: Any) -> List[Dict[str, Any]]:
    """
    Java 端 dimensionAnalysis 为 List<DimensionAnalysis>；模型常误输出 { \"因子\": 分 } 对象。
    """
    if val is None:
        return []
    if isinstance(val, dict):
        if _KNOWN_DIMENSION_KEYS.intersection(val.keys()):
            one = _normalize_one_dimension_item(val)
            return [one] if one else []
        return _dimension_analysis_rows_from_map(val)
    if isinstance(val, list):
        out: List[Dict[str, Any]] = []
        for it in val:
            if not isinstance(it, dict):
                continue
            if _KNOWN_DIMENSION_KEYS.intersection(it.keys()):
                one = _normalize_one_dimension_item(it)
                if one:
                    out.append(one)
            else:
                out.extend(_dimension_analysis_rows_from_map(it))
        return out
    return []


def _normalize_risk_level_for_backend(val: Any) -> str:
    """Java health_ai_analysis.risk_level 枚举：低|中|高|极高。"""
    if val is None:
        return "中"
    s = str(val).strip()
    if s in _BACKEND_RISK_LEVELS:
        return s
    sl = s.lower()
    mapping = {
        "中等": "中",
        "中度": "中",
        "轻度": "低",
        "轻微": "低",
        "重度": "高",
        "严重": "高",
        "极高": "极高",
        "很高": "高",
        "低危": "低",
        "中危": "中",
        "高危": "高",
    }
    if s in mapping:
        return mapping[s]
    if "轻" in s:
        return "低"
    if "重" in s or "极" in s:
        return "高" if "极" not in s else "极高"
    if "中" in s:
        return "中"
    # 模型把题号区间写进 riskLevel等无效值
    if any(c.isdigit() for c in s) and len(s) <= 12:
        return "中"
    return "中"


def _sanitize_dimension_analysis_evidence(analysis: Dict[str, Any]) -> None:
    """将 evidence 数组中的纯字符串项转为 EvidenceItem 对象，避免 Java 反序列化失败。"""
    dims = analysis.get("dimensionAnalysis")
    if not isinstance(dims, list):
        return
    for dim in dims:
        if not isinstance(dim, dict):
            continue
        ev = dim.get("evidence")
        if not isinstance(ev, list):
            continue
        fixed: List[Dict[str, str]] = []
        for i, e in enumerate(ev):
            if isinstance(e, dict):
                fixed.append(
                    {
                        "indicator": str(e.get("indicator", "") or ""),
                        "value": "" if e.get("value") is None else str(e.get("value")),
                        "reference": str(e.get("reference", "") or ""),
                    }
                )
            elif isinstance(e, str) and e.strip():
                fixed.append(
                    {
                        "indicator": f"条目{i + 1}",
                        "value": e.strip()[:500],
                        "reference": "",
                    }
                )
        dim["evidence"] = fixed
        dim["riskLevel"] = _normalize_risk_level_for_backend(dim.get("riskLevel"))


def _fix_abnormal_indicator_names(analysis: Dict[str, Any]) -> None:
    """补全 indicatorName，便于后端写入摘要与推断疾病兜底。"""
    ab = analysis.get("abnormalIndicators")
    if not isinstance(ab, list):
        return
    for item in ab:
        if not isinstance(item, dict):
            continue
        name = str(item.get("indicatorName") or "").strip()
        if name:
            continue
        v = item.get("value")
        code = item.get("indicatorCode")
        if v not in (None, ""):
            item["indicatorName"] = str(v).strip()[:120]
        elif code not in (None, ""):
            item["indicatorName"] = str(code).strip()[:120]


def _extract_hamd_like_score_from_archive(archive: Optional[Dict[str, Any]]) -> Optional[int]:
    """从建档 JSON 的 examinations 等字段中尽量解析 HAMD/汉密顿 总分。"""
    if not archive or not isinstance(archive, dict):
        return None
    scores: List[int] = []
    ex = archive.get("examinations")
    if isinstance(ex, dict):
        ex = [ex]
    if not isinstance(ex, list):
        ex = []

    def _try_int(v: Any) -> Optional[int]:
        if isinstance(v, bool):
            return None
        if isinstance(v, int):
            return v
        if isinstance(v, float):
            return int(round(v))
        if isinstance(v, str):
            t = v.strip()
            if t.isdigit():
                return int(t)
            # "41分" 等
            m = re.search(r"(\d{1,3})", t)
            if m:
                return int(m.group(1))
        return None

    for item in ex:
        if not isinstance(item, dict):
            continue
        blob = json.dumps(item, ensure_ascii=False)
        if not any(k in blob for k in ("HAMD", "汉密顿", "抑郁量表", "HAM-D")):
            continue
        for key in ("score", "result", "totalScore", "total", "rawScore"):
            n = _try_int(item.get(key))
            if n is not None and 0 <= n <= 80:
                scores.append(n)
                break
        res = item.get("results")
        if isinstance(res, dict):
            for key in ("总分", "total", "score", "合计"):
                n = _try_int(res.get(key))
                if n is not None and 0 <= n <= 80:
                    scores.append(n)
                    break
    return max(scores) if scores else None


def _try_int_hamd_range(v: Any, lo: int = 0, hi: int = 80) -> Optional[int]:
    """将常见数值/字符串转为 int，且落在量表合理区间（默认 HAMD 粗分）。"""
    if isinstance(v, bool):
        return None
    if isinstance(v, int):
        return v if lo <= v <= hi else None
    if isinstance(v, float):
        n = int(round(v))
        return n if lo <= n <= hi else None
    if isinstance(v, str):
        t = v.strip()
        if t.isdigit():
            n = int(t)
            return n if lo <= n <= hi else None
        m = re.search(r"(\d{1,3})", t)
        if m:
            n = int(m.group(1))
            return n if lo <= n <= hi else None
    return None


def _extract_hamd_score_from_overall_assessment(oa: Optional[Dict[str, Any]]) -> Optional[int]:
    """修复轮次常把 HAMD 总分放在 overallAssessment.score。"""
    if not isinstance(oa, dict):
        return None
    for key in ("score", "hamdScore", "totalScore", "hamd", "total", "rawScore"):
        n = _try_int_hamd_range(oa.get(key))
        if n is not None:
            return n
    return None


def _risk_hint_from_depression_text(text: str) -> str:
    """无具体分值的说明文中粗判风险（兜底）。"""
    if not text:
        return "中"
    if any(x in text for x in ("自杀", "自伤", "轻生")):
        return "极高"
    if any(x in text for x in ("重度", "严重抑郁", "严重的抑郁", "严重")):
        return "高"
    if any(x in text for x in ("中度", "明显抑郁")):
        return "中"
    if any(x in text for x in ("轻度", "较轻")):
        return "低"
    if "抑郁" in text or "HAMD" in text.upper() or "汉密顿" in text:
        return "中"
    return "中"


def _normalize_overall_assessment_for_java(analysis: Dict[str, Any]) -> None:
    """
    Java 端只认 overallAssessment.summary / riskLevel / overallHealthScore / keyRisks。
    小模型常输出 score、status、notes 等非标准字段，在此合并为可落库形态。
    """
    oa = analysis.get("overallAssessment")
    if not isinstance(oa, dict):
        return

    if not (str(oa.get("summary") or "").strip()):
        parts: List[str] = []
        for k in ("status", "notes", "description", "conclusion", "评估结论"):
            v = oa.get(k)
            if isinstance(v, str) and v.strip():
                parts.append(v.strip())
        if parts:
            oa["summary"] = " ".join(parts)[:2000]

    sc = _extract_hamd_score_from_overall_assessment(oa)
    blob = " ".join(
        str(oa.get(k) or "") for k in ("summary", "status", "notes", "description")
    )

    if sc is not None:
        rl, sm, hs = _hamd_summary_and_risk(sc)
        if not (str(oa.get("summary") or "").strip()):
            oa["summary"] = sm
        cur_rl = oa.get("riskLevel")
        oa["riskLevel"] = _normalize_risk_level_for_backend(cur_rl or rl)
        if oa.get("riskLevel") == "低" and rl in ("高", "极高"):
            oa["riskLevel"] = rl
        if oa.get("overallHealthScore") is None:
            oa["overallHealthScore"] = hs
        kr = oa.get("keyRisks")
        if not isinstance(kr, list) or not kr:
            oa["keyRisks"] = ["抑郁相关症状需关注", f"HAMD粗分约{sc}"]
    else:
        oa["riskLevel"] = _normalize_risk_level_for_backend(
            oa.get("riskLevel") or _risk_hint_from_depression_text(blob)
        )
        if not (str(oa.get("summary") or "").strip()) and blob.strip():
            oa["summary"] = blob.strip()[:2000]
        kr = oa.get("keyRisks")
        if (not isinstance(kr, list) or not kr) and any(
            x in blob for x in ("抑郁", "HAMD", "汉密顿", "焦虑")
        ):
            oa["keyRisks"] = ["情绪心理相关指标需关注"]

    if oa.get("overallHealthScore") is None:
        oa["overallHealthScore"] = 70

    kr = oa.get("keyRisks")
    if not isinstance(kr, list):
        oa["keyRisks"] = []
    oa["riskLevel"] = _normalize_risk_level_for_backend(oa.get("riskLevel"))


def _hamd_summary_and_risk(score: int) -> tuple:
    """返回 (risk_level, summary_line, health_score)。"""
    if score <= 7:
        rl = "低"
        sm = f"汉密顿抑郁量表（HAMD）粗分约 {score} 分，提示抑郁相关症状较轻，仍需结合临床面评。"
    elif score <= 16:
        rl = "中"
        sm = f"HAMD 粗分约 {score} 分，提示存在轻至中度抑郁相关症状，建议精神心理专科随访。"
    elif score <= 23:
        rl = "高"
        sm = f"HAMD 粗分约 {score} 分，提示中重度抑郁相关可能，建议尽快专科评估与干预。"
    else:
        rl = "极高"
        sm = f"HAMD 粗分约 {score} 分，提示重度抑郁相关可能较高，请务必尽快就医，本分析不能替代诊断。"
    hs = max(20, min(95, 100 - int(score * 1.15)))
    return rl, sm, hs


def _enrich_analysis_from_archive(
    analysis: Dict[str, Any], archive: Optional[Dict[str, Any]]
) -> None:
    """
    小模型常留下空的 overallAssessment / 非法 riskLevel / 混乱 evidence。
    用档案中的量表分值补全 health_ai_analysis 主表字段，并补充异常指标供推断疾病。
    """
    oa = analysis.get("overallAssessment")
    if not isinstance(oa, dict):
        oa = {}
        analysis["overallAssessment"] = oa

    hamd_arc = _extract_hamd_like_score_from_archive(archive)
    hamd_oa = _extract_hamd_score_from_overall_assessment(oa)
    if hamd_arc is not None and hamd_oa is not None:
        hamd = max(hamd_arc, hamd_oa)
    else:
        hamd = hamd_arc if hamd_arc is not None else hamd_oa

    if hamd is not None:
        rl, sm, hs = _hamd_summary_and_risk(hamd)
        if not (oa.get("summary") or "").strip():
            oa["summary"] = sm
        oa["riskLevel"] = _normalize_risk_level_for_backend(oa.get("riskLevel") or rl)
        # 若模型风险与量表严重不一致，以量表为准
        if oa.get("riskLevel") == "低" and rl in ("高", "极高"):
            oa["riskLevel"] = rl
        if oa.get("overallHealthScore") is None:
            oa["overallHealthScore"] = hs
        kr = oa.get("keyRisks")
        if not isinstance(kr, list) or not kr:
            oa["keyRisks"] = ["抑郁相关症状需关注", f"HAMD粗分约{hamd}"]
        # 保证有一条可被后端兜底抽取的异常指标名称（推断疾病）
        ab = analysis.get("abnormalIndicators")
        if not isinstance(ab, list):
            ab = []
            analysis["abnormalIndicators"] = ab
        has_hamd_row = any(
            isinstance(x, dict)
            and "HAMD" in str(x.get("indicatorName", "")).upper() + str(x.get("indicatorCode", "")).upper()
            for x in ab
        )
        if not has_hamd_row:
            ab.insert(
                0,
                {
                    "indicatorCode": "HAMD",
                    "indicatorName": "汉密顿抑郁量表",
                    "value": str(hamd),
                    "reference": "粗分，需结合临床",
                    "severity": "中度" if hamd <= 23 else "重度",
                    "relatedDimension": "PSYCHOLOGICAL",
                },
            )
    else:
        oa["riskLevel"] = _normalize_risk_level_for_backend(oa.get("riskLevel"))
        if oa.get("overallHealthScore") is None:
            oa["overallHealthScore"] = 70
        if not (oa.get("summary") or "").strip():
            oa["summary"] = "已根据上传资料完成结构化梳理；具体诊断请以医疗机构面诊为准。"

    # 诊断对象里含抑郁症等，补充 keyRisks / 推断线索
    if archive and isinstance(archive.get("diagnoses"), dict):
        for k, v in archive["diagnoses"].items():
            if not isinstance(k, str):
                continue
            blob = json.dumps(v, ensure_ascii=False) if v is not None else ""
            if "抑郁" in k + blob:
                kr = oa.get("keyRisks")
                if isinstance(kr, list) and "抑郁症相关" not in "".join(str(x) for x in kr):
                    kr.append("抑郁症相关评估线索")
                break


def _normalize_analysis_result_for_java_api(analysis: Dict[str, Any]) -> None:
    """就地修正 analysisResult，避免提交时 Jackson 反序列化失败。"""
    da = analysis.get("dimensionAnalysis")
    normalized = _normalize_dimension_analysis_for_api(da)
    analysis["dimensionAnalysis"] = normalized
    if isinstance(da, dict) and not _KNOWN_DIMENSION_KEYS.intersection(da.keys()):
        logger.info(
            "已将 dimensionAnalysis 从对象映射规范化为 %s 条维度记录（供 Java List反序列化）",
            len(normalized),
        )


def _coerce_health_analysis_top_level(analysis: Dict[str, Any]) -> None:
    """
    修复轮次常把 analysisMeta / overallAssessment / modelConfidence 写成空串，
    或把 evidence 写成字符串，导致下游 .get 与 Java 反序列化失败。
    """
    coerced = False

    am = analysis.get("analysisMeta")
    if not isinstance(am, dict):
        coerced = True
        if isinstance(am, str) and am.strip():
            analysis["analysisMeta"] = {
                "sourceType": "PHYSICAL_REPORT",
                "summaryHint": am.strip()[:800],
            }
        else:
            analysis["analysisMeta"] = {}

    oa = analysis.get("overallAssessment")
    if not isinstance(oa, dict):
        coerced = True
        if isinstance(oa, str) and oa.strip():
            analysis["overallAssessment"] = {
                "summary": oa.strip()[:800],
                "riskLevel": "中",
                "keyRisks": [],
            }
        else:
            analysis["overallAssessment"] = {
                "summary": "",
                "riskLevel": "中",
                "keyRisks": [],
            }

    mc = analysis.get("modelConfidence")
    if not isinstance(mc, dict):
        coerced = True
        if isinstance(mc, str) and mc.strip():
            try:
                parsed = json.loads(mc.strip())
                analysis["modelConfidence"] = (
                    parsed if isinstance(parsed, dict) else {"overallConfidence": 0.5, "limitations": []}
                )
            except Exception:
                analysis["modelConfidence"] = {
                    "overallConfidence": 0.5,
                    "limitations": [mc.strip()[:300]],
                }
        else:
            analysis["modelConfidence"] = {"overallConfidence": 0.5, "limitations": []}

    ai = analysis.get("abnormalIndicators")
    if not isinstance(ai, list):
        coerced = True
        analysis["abnormalIndicators"] = []
    else:
        fixed: List[Dict[str, Any]] = []
        for it in ai:
            if isinstance(it, dict):
                fixed.append(it)
            elif isinstance(it, str) and it.strip():
                coerced = True
                fixed.append(
                    {
                        "indicatorName": it.strip(),
                        "indicatorCode": "",
                        "value": "",
                        "severity": "轻度",
                    }
                )
        if len(fixed) != len(ai):
            coerced = True
        analysis["abnormalIndicators"] = fixed

    if coerced:
        logger.info("健康分析 JSON 顶层字段已归一化为对象/数组形状（兼容小模型修复输出）")


def _shared_health_record_storage() -> HealthRecordStorage:
    """与 ChatOrchestrator 共用内存档案，避免重建后分析读到空存储。"""
    try:
        from app.api import routes as _routes

        orch = getattr(_routes, "_orchestrator", None)
        st = getattr(orch, "health_record_storage", None) if orch else None
        if st is not None:
            return st
    except Exception:
        pass
    return HealthRecordStorage()


def generate_health_analysis(user_id: int, llm: LlmClient) -> dict:
    """
    生成健康分析结果
    
    基于用户的健康档案信息和体检报告原始数据，生成结构化的健康分析结果。
    
    Args:
        user_id: 用户ID
        llm: OpenAI 兼容 LLM 客户端（如本地 Ollama）
    
    Returns:
        结构化的健康分析结果 JSON 字典，包含以下字段：
        {
            "analysisMeta": {...},
            "overallAssessment": {...},
            "dimensionAnalysis": [...],
            "abnormalIndicators": [...],
            "modelConfidence": {...}
        }
    
    Raises:
        RuntimeError: 当数据读取或处理过程中发生错误时
        ValueError: 当 LLM 返回的 JSON 无法解析时
    """
    logger.info(f"开始为用户 {user_id} 生成健康分析结果")
    
    try:
        # 1. 读取健康档案 JSON
        health_record_storage = _shared_health_record_storage()
        archive_content = health_record_storage.get_record(
            user_id=user_id,
            record_id=_ARCHIVE_RECORD_ID
        )
        
        archive_json = None
        if archive_content:
            try:
                archive_json = json.loads(archive_content)
                logger.debug(f"成功读取用户 {user_id} 的健康档案")
                log_archive_json_summary(user_id, "storage_latest", archive_json)
            except json.JSONDecodeError as e:
                logger.warning(f"解析用户 {user_id} 的健康档案 JSON 失败: {str(e)}")
        
        # 2. 读取体检原始数据
        health_data_client = HealthDataClient()
        records = health_data_client.fetch_user_health_data(user_id)
        
        if not records:
            logger.warning(f"用户 {user_id} 没有健康数据")
            raise RuntimeError(f"用户 {user_id} 暂无健康数据记录")
        
        # 3. 解码健康数据
        data_processor = DataProcessor()
        decoded_records = []
        for idx, record in enumerate(records):
            if isinstance(record, dict):
                log_raw_incoming(user_id, idx, record)
            decoded_data = data_processor.decode_raw_data(
                record, pipeline_user_id=user_id, pipeline_idx=idx
            )
            if decoded_data is not None:
                decoded_records.append({
                    "dataType": record.get("dataType", "UNKNOWN"),
                    "formatType": record.get("formatType", "UNKNOWN"),
                    "decoded_data": decoded_data
                })
        
        if not decoded_records:
            logger.warning(f"用户 {user_id} 的健康数据解码后为空")
            raise RuntimeError(f"用户 {user_id} 的健康数据无法解析")
        
        # 4. 构建多模态内容（返回 OpenAI/Qwen-VL 兼容格式）
        contents = build_medical_contents(decoded_records)
        log_medical_contents(user_id, "health_analysis", contents)
        
        if not contents:
            logger.warning(f"用户 {user_id} 的多模态内容构建后为空")
            raise RuntimeError(f"用户 {user_id} 的健康数据无法转换为可用内容")
        
        # 5. 构建输入数据文本
        # 将健康档案 JSON 和原始数据内容组合成输入文本
        input_data_parts = []
        
        if archive_json:
            # 添加健康档案信息
            archive_text = json.dumps(archive_json, ensure_ascii=False, indent=2)
            input_data_parts.append(f"【健康档案信息】\n{archive_text}\n")
        
        # 添加体检原始数据（文本部分）
        text_contents = []
        for content in contents:
            if content.get("type") == "text":
                text_contents.append(content.get("text", ""))
        
        if text_contents:
            raw_data_text = "\n".join(text_contents)
            input_data_parts.append(f"【体检报告原始数据】\n{raw_data_text}\n")
        
        # 如果没有文本内容，至少添加一个说明
        if not input_data_parts:
            input_data_parts.append("【数据说明】\n用户提供了健康数据，但主要为图片格式。\n")
        
        input_data = "\n".join(input_data_parts)
        n_img = sum(1 for c in contents if c.get("type") == "image_url")
        raw_txt_len = sum(len(c.get("text") or "") for c in contents if c.get("type") == "text")
        arch_len = len(json.dumps(archive_json, ensure_ascii=False)) if archive_json else 0
        log_analysis_input_sizes(user_id, arch_len, raw_txt_len, n_img)
        
        # 6. 构建完整的 Prompt
        full_prompt = HEALTH_ANALYSIS_PROMPT_V1 + input_data
        
        # 7. 构建多模态消息
        # 将 contents 转换为 build_medical_messages 期望的格式
        formatted_contents = []
        
        # 先添加文本内容（包含 Prompt 和输入数据）
        formatted_contents.append({
            "type": "text",
            "data": full_prompt
        })
        
        # 再添加图片内容
        for content in contents:
            if content.get("type") == "image_url":
                image_url = content.get("image_url", {}).get("url", "")
                formatted_contents.append({
                    "type": "image",
                    "data": image_url
                })
        
        messages = build_medical_messages("", formatted_contents)
        
        # 8. 调用 LLM 生成结构化 JSON
        llm_response = llm.generate(
            messages,
            trace_user_id=user_id,
            trace_label="health_structured_analysis",
        )
        
        # 9. 解析 LLM 返回的 JSON（小模型常输出说明文字，失败时做一次纯文本 JSON 修复）
        try:
            analysis_result = _parse_json_response(llm_response)
        except ValueError as first_err:
            logger.warning(
                "用户 %s 健康分析首轮输出无法解析为 JSON，尝试文本修复: %s",
                user_id,
                first_err,
            )
            analysis_result = _repair_health_analysis_json(llm_response, user_id, llm)
            if analysis_result is None:
                error_msg = f"LLM 返回的 JSON 格式无效: {str(first_err)}"
                logger.error(error_msg)
                raise ValueError(error_msg) from first_err

        _coerce_health_analysis_top_level(analysis_result)
        _normalize_overall_assessment_for_java(analysis_result)
        _normalize_analysis_result_for_java_api(analysis_result)
        _sanitize_dimension_analysis_evidence(analysis_result)
        _fix_abnormal_indicator_names(analysis_result)
        _enrich_analysis_from_archive(analysis_result, archive_json)
        
        # 10. 处理 extendedData
        if "extendedData" not in analysis_result:
            # LLM 没有生成 extendedData，使用默认结构
            logger.info(f"LLM 未生成 extendedData，使用默认结构填充")
            analysis_result["extendedData"] = build_default_extended_data()
        else:
            # 合并 LLM 生成的数据和默认值
            default_data = build_default_extended_data()
            analysis_result["extendedData"] = merge_extended_data(
                analysis_result["extendedData"],
                default_data
            )
        
        # 11. 验证 extendedData 完整性
        if not validate_extended_data(analysis_result["extendedData"]):
            logger.warning(f"用户 {user_id} 的 extendedData 字段不完整，已使用默认值填充")

        # 12. 生成社团画像关键特征，供后端社团划分与病友推荐使用
        analysis_meta = analysis_result.get("analysisMeta") if isinstance(analysis_result.get("analysisMeta"), dict) else {}
        analysis_meta["communityFeatures"] = _build_community_features(analysis_result)
        analysis_result["analysisMeta"] = analysis_meta
        
        logger.info(f"用户 {user_id} 的健康分析结果生成完成")
        logger.debug(f"分析结果包含字段: {list(analysis_result.keys())}")
        
        return analysis_result
        
    except (ValueError, RuntimeError):
        # 重新抛出已知的异常
        raise
    except Exception as e:
        error_msg = f"生成健康分析结果失败: {str(e)}"
        logger.error(error_msg, exc_info=True)
        raise RuntimeError(error_msg) from e


def _extract_json_object_string(llm_response: str) -> Optional[str]:
    """从 LLM 输出中提取 JSON 对象子串（支持 ```json 围栏或首 { 至末 }）。"""
    raw = (llm_response or "").strip()
    if not raw:
        return None
    fenced = re.search(r"```(?:json)?\s*([\s\S]*?)\s*```", raw, re.IGNORECASE)
    if fenced:
        inner = fenced.group(1).strip()
        if inner.startswith("{"):
            return inner
    start = raw.find("{")
    end = raw.rfind("}")
    if start != -1 and end != -1 and end > start:
        return raw[start : end + 1]
    return None


def _repair_health_analysis_json(
    raw_text: str, user_id: int, llm: LlmClient
) -> Optional[Dict[str, Any]]:
    """多模态模型若输出说明文，用纯文本二次调用压缩为严格 JSON（不再次传图）。"""
    if not raw_text or not str(raw_text).strip():
        return None
    snippet = str(raw_text).strip()
    if len(snippet) > 14000:
        snippet = snippet[:14000] + "\n…(截断)"
    repair_prompt = (
        "你是医疗数据结构化助手。下面文本是对检查单/档案的描述或混杂输出。\n"
        "请输出【且仅输出】一个可被 json.loads 解析的 JSON 对象，不要 markdown、不要代码块、不要任何前后说明。\n"
        "顶层必须包含键：analysisMeta, overallAssessment, dimensionAnalysis, abnormalIndicators, modelConfidence。\n"
        "dimensionAnalysis 必须是 JSON 数组 [{...},...]，每项含 dimensionName、riskLevel、evidence；"
        "evidence 必须是数组 [{\"indicator\",\"value\",\"reference\"}]，禁止用字符串代替。\n"
        "analysisMeta、overallAssessment、modelConfidence 必须是 JSON 对象 {...}，禁止用空字符串 \"\" 代替。\n"
        "overallAssessment 必须使用标准键：summary（字符串）、riskLevel（仅 低|中|高|极高 之一）、"
        "overallHealthScore（0-100 整数）、keyRisks（字符串数组）；可把量表总分写在 summary 或 keyRisks 中，"
        "不要只用 score/status替代 summary。\n"
        "禁止输出 {\"因子名\": 分数} 这种对象形式（后端需要数组）。\n"
        "extendedData 若无法从原文推断，可省略（下游会补默认）。\n"
        "字段含义与取值约束须与常见体检/心理量表分析一致；禁止编造原文未出现的检验数值。\n\n"
        "待转换内容：\n"
        f"{snippet}"
    )
    try:
        fixed = llm.generate(
            [{"role": "user", "content": repair_prompt}],
            trace_user_id=user_id,
            trace_label="health_analysis_json_repair",
        )
        return _parse_json_response(fixed)
    except Exception:
        logger.warning("健康分析 JSON 修复调用失败", exc_info=True)
        return None


def _parse_json_response(llm_response: str) -> Dict[str, Any]:
    """
    解析 LLM 返回的 JSON 字符串（先抽取 {...} 再 json.loads）。
    """
    json_str = _extract_json_object_string(llm_response)
    if json_str is None:
        raise ValueError("响应中未找到 JSON 对象（无「{ … }」片段）")

    try:
        result = json.loads(json_str)
    except json.JSONDecodeError as e:
        logger.error("JSON 解析失败: %s", e)
        logger.debug("尝试解析的字符串(前500): %s...", json_str[:500])
        logger.debug("完整响应(前800): %s...", (llm_response or "")[:800])
        raise ValueError(
            f"无法解析 LLM 返回的 JSON 格式。错误详情: {str(e)}。"
        ) from e

    if not isinstance(result, dict):
        raise ValueError(
            f"LLM 返回的不是 JSON 对象，而是 {type(result).__name__} 类型"
        )

    return result


def _build_community_features(analysis_result: Dict[str, Any]) -> Dict[str, Any]:
    """从分析结果抽取社团划分所需关键特征。"""
    overall = analysis_result.get("overallAssessment", {}) if isinstance(analysis_result, dict) else {}
    if not isinstance(overall, dict):
        overall = {}
    risk_level = _normalize_risk_level_for_backend(overall.get("riskLevel", "中"))
    key_risks = overall.get("keyRisks", []) if isinstance(overall.get("keyRisks"), list) else []

    abnormal = analysis_result.get("abnormalIndicators", []) if isinstance(analysis_result.get("abnormalIndicators"), list) else []
    inferred = []
    used = set()
    for item in abnormal:
        if not isinstance(item, dict):
            continue
        name = str(item.get("indicatorName") or item.get("indicatorCode") or "").strip()
        if not name or name in used:
            continue
        used.add(name)
        inferred.append({"name": name, "confidence": 0.6})
        if len(inferred) >= 5:
            break

    summary_text = str(overall.get("summary") or "")
    psych_extra: List[tuple] = []
    if risk_level in ("高", "极高"):
        psych_extra.append(("抑郁相关风险（建议精神心理科评估）", 0.68))
    if any(k in summary_text for k in ("HAMD", "汉密顿", "抑郁量表", "抑郁")):
        psych_extra.append(("抑郁相关症状（量表/筛查提示）", 0.65))
    for pname, pconf in psych_extra:
        if pname in used or len(inferred) >= 5:
            continue
        used.add(pname)
        inferred.insert(0, {"name": pname, "confidence": pconf})

    ext = analysis_result.get("extendedData", {}) if isinstance(analysis_result.get("extendedData"), dict) else {}
    lifestyle = ext.get("lifestyleStatus", {}) if isinstance(ext.get("lifestyleStatus"), dict) else {}

    return {
        "diseaseRisk": {
            "confirmedDiseases": [],
            "inferredDiseases": inferred,
            "riskLevel": risk_level,
            "comorbidityPattern": key_risks[:3] if key_risks else []
        },
        "behavior": {
            "exerciseLevel": lifestyle.get("exerciseFrequency"),
            "sleepQuality": lifestyle.get("sleepStatus"),
            "smokingDrinkingLevel": f"{lifestyle.get('smokingStatus')}/{lifestyle.get('drinkingStatus')}",
            "dietPattern": lifestyle.get("dietType"),
            "adherenceScore": 0.5,
            "followupRegularity": 0.5
        },
        "metricTrend": {
            "trendStatus": "stable",
            "keyMetricCount": len(abnormal)
        },
        "stageNeed": {
            "currentGoals": key_risks[:5] if key_risks else ["健康管理"],
            "careStage": "稳定管理期"
        },
        "socialTalkability": {
            "activeTimeWindow": "19:00-22:00",
            "replyStyle": "normal"
        }
    }
