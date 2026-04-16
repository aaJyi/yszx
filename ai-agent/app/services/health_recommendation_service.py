"""
健康建议生成服务
用于基于健康分析结果生成健康管理建议
"""
import json
import re
import logging
from typing import Dict, Any, Optional, List

try:
    import json_repair
except ImportError:
    json_repair = None

from app.db.health_archive_process_client import fetch_latest_health_analysis
from app.core.medical_prompts import HEALTH_RECOMMENDATION_PROMPT_V1
from app.core.llm_client import LlmClient
from app.core.config import settings

# 配置日志
logger = logging.getLogger(__name__)

# 健康建议仅文本；超长 extendedData + VL 模型易触发 Ollama「GGML_ASSERT…failed」
_RECOMMENDATION_ANALYSIS_MAX_JSON_CHARS = 8000


def _guess_recommendation_category_cn(title: str) -> str:
    """从标题粗映射到 prompt 要求的中文 category（供 convert 映射为枚举）。"""
    if not title:
        return "生活方式"
    if any(k in title for k in ("饮食", "营养", "减重", "肥胖", "碳水")):
        return "饮食"
    if any(k in title for k in ("运动", "锻炼", "有氧", "活动量")):
        return "运动"
    if any(k in title for k in ("睡眠", "作息", "休息")):
        return "生活方式"
    if any(k in title for k in ("医疗", "医生", "专科", "就诊", "医院", "检查")):
        return "医疗"
    if any(k in title for k in ("复查", "随访", "监测")):
        return "复查建议"
    if any(k in title for k in ("风险", "注意", "警惕")):
        return "风险提示"
    if any(k in title for k in ("心理", "情绪", "焦虑", "抑郁", "压力", "正念")):
        return "生活方式"
    return "生活方式"


def _fallback_recommendation_from_plain_text(
    llm_response: str,
    analysis_result: Optional[Dict[str, Any]],
) -> Dict[str, Any]:
    """
    小文本模型（如 deepseek-r1）常无视「仅 JSON」指令而输出 Markdown。
    将编号 +加粗标题结构拆成 recommendationItems，保证下游可入库。
    """
    text = _strip_reasoning_prefix((llm_response or "").strip())
    meta = {
        "basedOnAnalysisId": "",
        "recommendationType": "AI_HEALTH_MANAGEMENT",
    }
    notice = {"medicalDisclaimer": "本建议仅用于健康管理参考，不替代医生诊断"}

    if not text:
        oa = (
            (analysis_result or {}).get("overallAssessment")
            if isinstance(analysis_result, dict)
            else None
        )
        summary = ""
        if isinstance(oa, dict):
            summary = str(oa.get("summary") or "").strip()[:500]
        return {
            "recommendationMeta": meta,
            "overallAdvice": {
                "summary": summary or "暂未能生成结构化建议，请稍后重试。",
                "priorityLevel": "中",
            },
            "recommendationItems": [],
            "notice": notice,
        }

    priority = "高" if any(x in text for x in ("严重", "重度", "紧急", "自杀", "自伤")) else "中"
    items: List[Dict[str, Any]] = []

    # 形如：1. **焦虑与压力缓解：**\n   - 要点…
    block_pat = re.compile(
        r"(?ms)^(\d+)\.\s*\*\*([^\*]+)\*\*\s*[：:]*\s*\n(.*?)(?=^\d+\.\s|\Z)"
    )
    for m in block_pat.finditer(text):
        title = (m.group(2) or "").strip()
        body = (m.group(3) or "").strip()
        body = re.sub(r"\n{3,}", "\n\n", body)
        if not title:
            continue
        cat = _guess_recommendation_category_cn(title)
        dim = None
        if any(k in title for k in ("心理", "情绪", "焦虑", "抑郁", "压力")):
            dim = "EMOTION"
        items.append(
            {
                "category": cat,
                "title": title[:120],
                "content": (body if body else title)[:8000],
                "confidence": 0.72,
                "dimensionCode": dim,
            }
        )

    # 无 **包裹的编号小节：1. 标题：\n 正文
    if not items:
        plain_pat = re.compile(
            r"(?ms)^(\d+)\.\s+([^\n\*]{2,80}?)\s*[：:]\s*\n(.*?)(?=^\d+\.\s|\Z)"
        )
        for m in plain_pat.finditer(text):
            title = (m.group(2) or "").strip()
            body = (m.group(3) or "").strip()
            body = re.sub(r"\n{3,}", "\n\n", body)
            if not title or title.startswith("**"):
                continue
            cat = _guess_recommendation_category_cn(title)
            dim = "EMOTION" if any(k in title for k in ("心理", "情绪", "焦虑", "抑郁")) else None
            items.append(
                {
                    "category": cat,
                    "title": title[:120],
                    "content": (body if body else title)[:8000],
                    "confidence": 0.7,
                    "dimensionCode": dim,
                }
            )

    summary_src = text[:300].replace("\n", " ").strip()
    if not items:
        items.append(
            {
                "category": "生活方式",
                "title": "健康管理建议",
                "content": text[:8000],
                "confidence": 0.65,
                "dimensionCode": "EMOTION" if "抑郁" in text or "焦虑" in text else None,
            }
        )
        summary_src = summary_src[:200]

    return {
        "recommendationMeta": meta,
        "overallAdvice": {
            "summary": summary_src[:240] if summary_src else "已根据分析生成条目化建议。",
            "priorityLevel": priority,
        },
        "recommendationItems": items[:12],
        "notice": notice,
    }


def _finalize_recommendation_result(result: Dict[str, Any]) -> None:
    """
    json_repair 后 recommendationItems 可能混入字符串；overallAdvice 也可能是纯文本。
    就地整理，避免 convert_recommendation_result_to_api_format 中 item.get 报错。
    """
    oa = result.get("overallAdvice")
    if isinstance(oa, str) and oa.strip():
        text = oa.strip()
        result["overallAdvice"] = {
            "summary": text[:8000],
            "priorityLevel": "高" if any(x in text for x in ("严重", "重度", "紧急", "自杀")) else "中",
        }
    elif not isinstance(oa, dict):
        result["overallAdvice"] = {}

    items = result.get("recommendationItems")
    if not isinstance(items, list):
        result["recommendationItems"] = []
        return

    fixed: List[Dict[str, Any]] = []
    for it in items:
        if isinstance(it, dict):
            fixed.append(it)
        elif isinstance(it, str) and it.strip():
            fixed.append(
                {
                    "category": "生活方式",
                    "title": "健康建议",
                    "content": it.strip()[:8000],
                    "priority": "中",
                }
            )
    result["recommendationItems"] = fixed


def _slim_analysis_for_recommendation(
    analysis_result: Dict[str, Any], max_chars: int = _RECOMMENDATION_ANALYSIS_MAX_JSON_CHARS
) -> Dict[str, Any]:
    """去掉过长的 extendedData 等，缩短纯文本 prompt，并减轻推理负担。"""
    if not isinstance(analysis_result, dict):
        return {}
    order_keys = (
        "analysisMeta",
        "overallAssessment",
        "dimensionAnalysis",
        "abnormalIndicators",
        "modelConfidence",
        "communityFeatures",
    )
    slim: Dict[str, Any] = {k: analysis_result[k] for k in order_keys if k in analysis_result}
    ext = analysis_result.get("extendedData")
    if isinstance(ext, dict):
        slim["extendedData"] = ext

    def _size(d: Dict[str, Any]) -> int:
        return len(json.dumps(d, ensure_ascii=False))

    while _size(slim) > max_chars:
        if slim.pop("extendedData", None) is not None:
            continue
        if slim.pop("communityFeatures", None) is not None:
            continue
        da = slim.get("dimensionAnalysis")
        if isinstance(da, list) and len(da) > 6:
            slim["dimensionAnalysis"] = da[:6]
            continue
        ai = slim.get("abnormalIndicators")
        if isinstance(ai, list) and len(ai) > 12:
            slim["abnormalIndicators"] = ai[:12]
            continue
        break

    if _size(slim) > max_chars:
        logger.warning(
            "健康建议输入仍超过 %s 字符（%s），将仅保留 overallAssessment 与 dimensionAnalysis 摘要",
            max_chars,
            _size(slim),
        )
        slim = {
            "overallAssessment": slim.get("overallAssessment"),
            "dimensionAnalysis": (slim.get("dimensionAnalysis") or [])[:4],
            "abnormalIndicators": (slim.get("abnormalIndicators") or [])[:8],
        }
    return slim


def generate_health_recommendation(user_id: int, analysis_result: Optional[Dict[str, Any]] = None) -> dict:
    """
    生成健康建议
    
    基于用户最新的健康分析结果，生成结构化的健康管理建议。
    
    Args:
        user_id: 用户ID
    
    Returns:
        结构化的健康建议结果 JSON 字典，包含以下字段：
        {
            "recommendationMeta": {...},
            "overallAdvice": {...},
            "recommendationItems": [...],
            "notice": {...}
        }
    
    Raises:
        RuntimeError: 当数据读取或处理过程中发生错误时
        ValueError: 当 LLM 返回的 JSON 无法解析时
    """
    logger.info(f"开始为用户 {user_id} 生成健康建议")
    
    try:
        # 1. 获取健康分析结果
        if analysis_result is None:
            # 如果没有提供分析结果，从数据库读取该用户最新的 health_ai_analysis
            analysis_data = fetch_latest_health_analysis(user_id)
            
            if not analysis_data:
                raise RuntimeError(f"用户 {user_id} 没有健康分析结果")
            
            # 提取 analysisResult 字段（如果存在）
            analysis_result = analysis_data.get("analysisResult")
            if not analysis_result:
                # 如果整个 analysis_data 就是分析结果，直接使用
                analysis_result = analysis_data
        
        # 2. 构建输入数据文本（压缩后再入模，避免超长 JSON）
        slim = _slim_analysis_for_recommendation(analysis_result)
        analysis_text = json.dumps(slim, ensure_ascii=False, indent=2)
        input_data = f"【健康分析结果】\n{analysis_text}\n"
        logger.info(
            "[PIPELINE] user=%s step=recommendation_input analysisJsonChars=%s (slim from full analysis)",
            user_id,
            len(analysis_text),
        )
        
        # 3. 构建完整的 Prompt
        full_prompt = HEALTH_RECOMMENDATION_PROMPT_V1 + input_data
        
        # 4. 构建消息（纯文本）
        messages = [
            {"role": "user", "content": full_prompt}
        ]
        
        # 5. 纯文本建议：使用 llm_model_text，且关闭 num_gpu 透传，降低 VL/长文本下的 GGML 崩溃概率
        text_model = (settings.llm_model_text or "").strip() or settings.llm_model
        if text_model == settings.llm_model and "vl" in settings.llm_model.lower():
            logger.warning(
                "LLM_MODEL_TEXT 未单独配置且与 VL 模型相同，请在 .env 设置 LLM_MODEL_TEXT 为已 ollama pull 的纯文本模型（如 deepseek-r1:1.5b）"
            )
        logger.info("健康建议使用文本模型: %s（Ollama extra_body 已关闭）", text_model)
        llm = LlmClient(model=text_model, ollama_runtime_options=False)
        llm_response = llm.generate(
            messages,
            trace_user_id=user_id,
            trace_label="health_recommendation",
        )
        
        # 6. 解析 LLM 返回的 JSON（失败时用 Markdown 文本兜底，避免整链失败）
        try:
            recommendation_result = _parse_json_response(llm_response)
        except ValueError as e:
            logger.warning(
                "用户 %s 健康建议模型未输出可解析 JSON（%s），改用纯文本/Markdown 兜底",
                user_id,
                e,
            )
            recommendation_result = _fallback_recommendation_from_plain_text(
                llm_response, analysis_result
            )

        _finalize_recommendation_result(recommendation_result)
        
        logger.info(f"用户 {user_id} 的健康建议生成完成")
        logger.debug(f"建议结果包含字段: {list(recommendation_result.keys())}")
        
        return recommendation_result
        
    except (ValueError, RuntimeError):
        # 重新抛出已知的异常
        raise
    except Exception as e:
        error_msg = f"生成健康建议失败: {str(e)}"
        logger.error(error_msg, exc_info=True)
        raise RuntimeError(error_msg) from e


def _strip_reasoning_prefix(text: str) -> str:
    """DeepSeek-R1 等模型可能在 JSON 前输出思考段，先去掉再解析。"""
    if not text:
        return text
    out = text
    for pat in (
        r"<\s*think\s*>[\s\S]*?<\s*/\s*think\s*>",
        r"【思考】[\s\S]*?【/思考】",
    ):
        out = re.sub(pat, "", out, flags=re.IGNORECASE)
    return out.strip()


def _extract_recommendation_json_string(llm_response: str) -> str:
    """
    从模型输出中取出 JSON 文本。
    注意：不能用 \\{.*?\\} 非贪婪匹配嵌套对象，应取 ```围栏内全文或首 { 至末 }。
    """
    raw = _strip_reasoning_prefix((llm_response or "").strip())
    fenced = re.search(r"```(?:json)?\s*([\s\S]*?)\s*```", raw, re.IGNORECASE)
    if fenced:
        return fenced.group(1).strip()
    start = raw.find("{")
    end = raw.rfind("}")
    if start != -1 and end != -1 and end > start:
        return raw[start : end + 1].strip()
    return raw


def _parse_json_response(llm_response: str) -> Dict[str, Any]:
    """
    解析 LLM 返回的 JSON（严格 json.loads，失败时用 json_repair 修复未转义换行等）。
    """
    json_str = _extract_recommendation_json_string(llm_response)
    if not json_str or not json_str.startswith("{"):
        raise ValueError("响应中未找到 JSON 对象")

    result: Any = None
    try:
        result = json.loads(json_str)
    except json.JSONDecodeError as e:
        if json_repair is not None:
            try:
                result = json_repair.loads(json_str)
                logger.info("健康建议 JSON 已由 json_repair 修复解析")
            except Exception as e2:
                logger.error("JSON 解析失败: %s；json_repair 亦失败: %s", e, e2)
                logger.debug("JSON 片段(前800): %s", json_str[:800])
                raise ValueError(
                    f"无法解析 LLM 返回的 JSON。详情: {str(e)}"
                ) from e
        else:
            logger.error("JSON 解析失败: %s（可 pip install json-repair）", e)
            logger.debug("JSON 片段(前800): %s", json_str[:800])
            raise ValueError(
                f"无法解析 LLM 返回的 JSON 格式。错误详情: {str(e)}。"
            ) from e

    if not isinstance(result, dict):
        raise ValueError(
            f"LLM 返回的不是 JSON 对象，而是 {type(result).__name__} 类型"
        )

    return result
