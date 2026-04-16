# -*- coding: utf-8 -*-
"""
健康档案编排服务（精简版）。

仅保留：
1) 健康档案重建（读取 raw-health-data -> LLM 整理结构化档案 -> 推送后端）
2) 健康档案分析（基于档案生成健康总结文本）

已移除所有对话相关方法。
"""
import json
import logging
import re
from typing import Any, Dict, Optional

try:
    import json_repair
except ImportError:
    json_repair = None

from app.core.data_processor import DataProcessor
from app.core.health_record_storage import HealthRecordStorage
from app.core.llm_client import LlmClient
from app.core.medical_content_builder import build_medical_contents
from app.core.medical_prompts import build_analyze_prompt, build_organize_prompt
from app.core.multimodal_builder import build_medical_messages
from app.db.health_archive_process_client import push_medical_report
from app.db.health_data_client import HealthDataClient
from app.core.pipeline_trace import (
    log_archive_json_summary,
    log_medical_contents,
    log_raw_incoming,
)

logger = logging.getLogger(__name__)


class ChatOrchestrator:
    """健康档案编排服务（无对话能力）。"""

    def __init__(
        self,
        llm: LlmClient,
        health_data_client: Optional[HealthDataClient] = None,
        health_record_storage: Optional[HealthRecordStorage] = None,
    ):
        self.llm = llm
        self.health_data_client = health_data_client or HealthDataClient()
        self.data_processor = DataProcessor()
        self.health_record_storage = health_record_storage or HealthRecordStorage()
        self._ARCHIVE_RECORD_ID = "latest"
        if json_repair is None:
            logger.warning(
                "未安装 json-repair，建档 JSON 容错下降；请执行 pip install -r requirements.txt"
            )
        logger.info("ChatOrchestrator(精简版) 初始化完成：仅健康档案能力")

    def _get_archive_status(self, user_id: int) -> Optional[str]:
        status = self.health_record_storage.get_archive_status(
            user_id=user_id,
            record_id=self._ARCHIVE_RECORD_ID,
        )
        logger.debug("用户 %s 健康档案状态: %s", user_id, status)
        return status

    def _save_archive(self, user_id: int, archive_json: Dict[str, Any]) -> None:
        content = json.dumps(archive_json, ensure_ascii=False)
        self.health_record_storage.save_record(
            user_id=user_id,
            record_id=self._ARCHIVE_RECORD_ID,
            content=content,
        )
        logger.info("用户 %s 健康档案已保存到本地存储", user_id)

    @staticmethod
    def _collapse_split_json_keys(s: str) -> str:
        """
        小模型偶发在键名中间换行，例如 "diagnose\\ns": ，标准 JSON 非法。
        将两段紧邻的键名片段合并为一段（仅匹配 "word\\nword": 形式）。
        """
        pattern = re.compile(
            r'"([A-Za-z0-9_\u4e00-\u9fff]+)\s*\r?\n\s*([A-Za-z0-9_\u4e00-\u9fff]+)"\s*:'
        )
        prev: Optional[str] = None
        out = s
        while prev != out:
            prev = out
            out = pattern.sub(r'"\1\2":', out)
        return out

    @staticmethod
    def _heuristic_fix_mixed_quotes(s: str) -> str:
        """
        小模型常见错误：字符串以英文双引号开头、以单引号收尾（如 "精神科',），标准 json 无法解析。
        将 : "……' 且其后（可跨空白行）为 , } ] 的片段改为合法双引号闭合（不处理值内含未转义 " 的情形）。
        """
        return re.sub(
            r':\s*"([^"]*?)\'(?=\s*[,}\]])',
            r': "\1"',
            s,
        )

    @classmethod
    def _try_parse_one_json_blob(cls, candidate: str) -> Optional[Dict[str, Any]]:
        if not candidate or not candidate.strip():
            return None
        c = candidate.strip()
        collapsed = cls._collapse_split_json_keys(c)
        variants = [
            c,
            collapsed,
            cls._heuristic_fix_mixed_quotes(c),
            cls._heuristic_fix_mixed_quotes(collapsed),
        ]
        for blob in variants:
            try:
                obj = json.loads(blob)
                if isinstance(obj, dict):
                    return obj
            except Exception:
                pass
            if json_repair is not None:
                try:
                    obj = json_repair.loads(blob)
                    if isinstance(obj, dict):
                        logger.info("建档 JSON 已由 json_repair 修复解析")
                        return obj
                except Exception:
                    pass
        return None

    @classmethod
    def _parse_archive_json(cls, text: str) -> Dict[str, Any]:
        """解析 LLM 输出，支持裸 JSON、```json 围栏、混用引号及 json_repair 兜底。"""
        if not text:
            raise ValueError("LLM 返回内容为空")

        raw = text.strip()
        parsed = cls._try_parse_one_json_blob(raw)
        if parsed is not None:
            return parsed

        fenced = re.search(r"```(?:json)?\s*([\s\S]*?)\s*```", raw, flags=re.IGNORECASE)
        if fenced:
            parsed = cls._try_parse_one_json_blob(fenced.group(1))
            if parsed is not None:
                return parsed

        start = raw.find("{")
        end = raw.rfind("}")
        if start != -1 and end != -1 and end > start:
            parsed = cls._try_parse_one_json_blob(raw[start : end + 1])
            if parsed is not None:
                return parsed
        raise ValueError("LLM 返回内容无法解析为 JSON 对象")

    def _repair_to_json(self, raw_text: str, user_id: int) -> Optional[Dict[str, Any]]:
        """
        当首轮输出不是严格 JSON 时，进行一次“仅格式修复”的二次调用。
        只允许返回 JSON 对象，不引入新事实。
        """
        if not raw_text:
            return None
        repair_prompt = (
            "请把下面内容修复为【严格 JSON 对象】并直接输出，不要任何解释、不要 markdown、不要 ```代码块。\n"
            "要求：\n"
            "1) 所有键与字符串值必须使用英文双引号 \" ，禁止用单引号 ' 作为字符串首尾；\n"
            "2) 只能做格式修复（引号配对、逗号、尾逗号、转义）；不得改字段语义与事实；\n"
            "3) 输出必须可被 json.loads 一次解析为 dict。\n\n"
            "待修复内容如下：\n"
            f"{raw_text}"
        )
        try:
            fixed = self.llm.generate(
                [{"role": "user", "content": repair_prompt}],
                trace_user_id=user_id,
                trace_label="archive_json_repair",
            )
            logger.info("用户建档 JSON 修复输出长度: %s", len(fixed or ""))
            return self._parse_archive_json(fixed)
        except Exception:
            logger.warning("建档 JSON 二次修复失败", exc_info=True)
            return None

    def generate_health_archive(self, user_id: int) -> Dict[str, Any]:
        """
        基于用户原始健康数据生成结构化健康档案。
        """
        records = self.health_data_client.fetch_user_health_data(user_id)
        if not records:
            raise RuntimeError(f"用户 {user_id} 没有原始健康数据")

        decoded_records = []
        for idx, rec in enumerate(records):
            log_raw_incoming(user_id, idx, rec if isinstance(rec, dict) else {})
            decoded = self.data_processor.decode_raw_data(
                rec, pipeline_user_id=user_id, pipeline_idx=idx
            )
            if decoded is None:
                continue
            decoded_records.append(
                {
                    "dataType": rec.get("dataType", "UNKNOWN"),
                    "formatType": rec.get("formatType", "UNKNOWN"),
                    "decoded_data": decoded,
                }
            )
        if not decoded_records:
            raise RuntimeError(f"用户 {user_id} 原始数据全部为空或不可解码")

        contents = build_medical_contents(decoded_records)
        log_medical_contents(user_id, "organize_archive", contents)
        prompt = build_organize_prompt(contents)
        messages = build_medical_messages(prompt, contents)
        llm_out = self.llm.generate(
            messages, trace_user_id=user_id, trace_label="health_archive_organize"
        )
        logger.info("用户 %s 建档 LLM 输出长度: %s", user_id, len(llm_out or ""))
        try:
            archive_json = self._parse_archive_json(llm_out)
        except Exception:
            repaired = self._repair_to_json(llm_out, user_id)
            if repaired is None:
                raise
            archive_json = repaired
        log_archive_json_summary(user_id, "organize_archive_done", archive_json)
        logger.info("用户 %s 健康档案生成完成", user_id)
        return archive_json

    def rebuild_health_archive(self, user_id: int) -> Dict[str, Any]:
        """
        仅允许在 DIRTY 状态触发重建：
        DIRTY -> UPDATING -> READY（失败回滚为 DIRTY）
        """
        current_status = self._get_archive_status(user_id)
        if current_status != "DIRTY":
            raise RuntimeError(
                f"用户 {user_id} 当前状态为 {current_status}，仅 DIRTY 允许重建"
            )

        ok = self.health_record_storage.set_archive_status(
            user_id=user_id,
            record_id=self._ARCHIVE_RECORD_ID,
            status="UPDATING",
        )
        if not ok:
            raise RuntimeError(f"用户 {user_id} 切换 UPDATING 失败")

        try:
            archive_json = self.generate_health_archive(user_id)
            # 推送到后端健康档案接口
            push_medical_report(user_id=user_id, report_json=archive_json)
            # 存一份本地 latest（供排障）
            self._save_archive(user_id, archive_json)
            self.health_record_storage.set_archive_status(
                user_id=user_id,
                record_id=self._ARCHIVE_RECORD_ID,
                status="READY",
            )
            self.health_record_storage.clear_refresh_flag(user_id)
            logger.info("用户 %s 健康档案重建成功", user_id)
            return archive_json
        except Exception:
            self.health_record_storage.set_archive_status(
                user_id=user_id,
                record_id=self._ARCHIVE_RECORD_ID,
                status="DIRTY",
            )
            logger.exception("用户 %s 健康档案重建失败，已回滚 DIRTY", user_id)
            raise

    def analyze_health_archive(
        self,
        archive_json: Dict[str, Any],
        question: Optional[str] = None,
        user_id: Optional[int] = None,
        use_raw_data: bool = False,
    ) -> str:
        """
        基于健康档案输出分析文本（保留原签名，兼容历史调用）。
        """
        if not isinstance(archive_json, dict):
            raise TypeError(
                f"archive_json 必须是 dict 类型，当前类型: {type(archive_json).__name__}"
            )
        archive_text = json.dumps(archive_json, ensure_ascii=False, indent=2)
        prompt = build_analyze_prompt(archive_text, question)
        messages = [{"role": "user", "content": prompt}]
        return self.llm.generate(
            messages,
            trace_user_id=user_id,
            trace_label="health_archive_analyze_text",
        )

