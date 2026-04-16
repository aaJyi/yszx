"""
核心配置和 LLM 接口
"""
from app.core.config import settings
from app.core.llm_client import LlmClient

__all__ = ["settings", "LlmClient"]
