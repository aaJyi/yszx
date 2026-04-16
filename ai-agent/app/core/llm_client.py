"""
OpenAI 兼容 API 客户端（本地 Ollama / vLLM / 其他云厂商统一接口，支持多模态）。

说明：是否使用 NVIDIA GPU 由 Ollama 进程与驱动决定；本客户端可向 Ollama 透传
`extra_body.options.num_gpu`，尽量把模型层放到 GPU（见 Ollama API 的 options）。
若 ollama ps 仍显示 100% CPU，请在 Windows「图形设置」中将 ollama.exe 设为高性能 NVIDIA。
"""
import os
import time
import logging
from typing import List, Dict, Any, Optional

import httpx

from app.core.pipeline_trace import log_llm_request, log_llm_response

try:
    from openai import OpenAI
    try:
        from openai import RateLimitError
    except ImportError:
        RateLimitError = None
    HAS_OPENAI = True
except ImportError:
    HAS_OPENAI = False
    RateLimitError = None

logger = logging.getLogger(__name__)


def _is_billing_quota_error(exc: Exception) -> bool:
    """云端 OpenAI 兼容接口欠费时常见 insufficient_quota；本地 Ollama 极少出现。"""
    s = str(exc).lower()
    if "insufficient_quota" in s:
        return True
    if "exceeded your current quota" in s:
        return True
    resp = getattr(exc, "response", None)
    if resp is not None:
        try:
            txt = (getattr(resp, "text", None) or "").lower()
            if "insufficient_quota" in txt or "exceeded your current quota" in txt:
                return True
        except Exception:
            pass
    return False


class LlmClient:
    """OpenAI SDK + 任意兼容 /v1/chat/completions 的后端（默认本地 Ollama）。"""

    def __init__(
        self,
        api_key: Optional[str] = None,
        base_url: Optional[str] = None,
        model: Optional[str] = None,
        ollama_runtime_options: Optional[bool] = None,
    ):
        if not HAS_OPENAI:
            raise ImportError("请安装 openai 库: pip install openai")

        try:
            from app.core.config import settings

            if api_key is None:
                api_key = settings.llm_api_key
            if base_url is None:
                base_url = settings.llm_base_url
            if model is None:
                model = settings.llm_model
        except Exception:
            pass

        api_key = api_key or os.getenv("LLM_API_KEY") or "ollama"
        base_url = base_url or os.getenv("LLM_BASE_URL") or "http://127.0.0.1:11434/v1"
        model = model or os.getenv("LLM_MODEL") or "qwen2.5vl:3b"

        timeout_sec = 600.0
        use_ollama_opts = False
        ollama_num_gpu = 99
        try:
            from app.core.config import settings

            timeout_sec = settings.llm_http_timeout_seconds
            if ollama_runtime_options is None:
                use_ollama_opts = settings.llm_ollama_runtime_options
            else:
                use_ollama_opts = bool(ollama_runtime_options)
            ollama_num_gpu = settings.llm_ollama_num_gpu
        except Exception:
            if ollama_runtime_options is not None:
                use_ollama_opts = bool(ollama_runtime_options)

        timeout = httpx.Timeout(timeout_sec, connect=30.0)
        self.client = OpenAI(
            api_key=api_key,
            base_url=base_url.rstrip("/"),
            timeout=timeout,
        )
        self.model = model
        self._ollama_extra_body: Optional[Dict[str, Any]] = None
        if use_ollama_opts:
            self._ollama_extra_body = {"options": {"num_gpu": ollama_num_gpu}}
            logger.info(
                "LLM Ollama extra: num_gpu=%s model=%s (非 Ollama 请设 llm_ollama_runtime_options=false)",
                ollama_num_gpu,
                self.model,
            )

    def _chat_kwargs(self) -> Dict[str, Any]:
        if self._ollama_extra_body:
            return {"extra_body": self._ollama_extra_body}
        return {}

    def generate(
        self,
        messages: List[Dict[str, Any]],
        max_retries: int = 3,
        initial_retry_delay: float = 1.0,
        trace_user_id: Optional[int] = None,
        trace_label: str = "llm",
    ) -> str:
        if not messages:
            raise ValueError("messages 不能为空")

        retry_delay = initial_retry_delay
        log_llm_request(trace_user_id, f"{trace_label} model={self.model}", messages)

        for attempt in range(max_retries + 1):
            try:
                t0 = time.perf_counter()
                completion = self.client.chat.completions.create(
                    model=self.model,
                    messages=messages,
                    **self._chat_kwargs(),
                )
                elapsed_ms = (time.perf_counter() - t0) * 1000.0
                out = completion.choices[0].message.content
                log_llm_response(trace_user_id, trace_label, elapsed_ms, out)
                return out

            except Exception as e:
                err_body = getattr(e, "body", None)
                if err_body is not None:
                    logger.error("LLM API error body: %s", err_body)
                resp = getattr(e, "response", None)
                if resp is not None:
                    try:
                        code = getattr(resp, "status_code", None)
                        txt = (getattr(resp, "text", None) or "")[:4000]
                        if txt or code:
                            logger.error(
                                "LLM HTTP 错误 status=%s text(截断)=%s",
                                code,
                                txt,
                            )
                    except Exception:
                        pass

                if _is_billing_quota_error(e):
                    msg = (
                        "LLM API：账户额度已用尽（insufficient_quota），"
                        "请检查云服务商计费或更换 Key；本地 Ollama 无此错误。"
                        "详情: "
                        + str(e)
                    )
                    logger.error(msg)
                    raise Exception(msg) from e

                is_rate_limit = False
                error_str = str(e).lower()
                if "429" in error_str or "rate_limit" in error_str or "too frequent" in error_str:
                    is_rate_limit = True
                if RateLimitError and isinstance(e, RateLimitError):
                    is_rate_limit = True

                if is_rate_limit and attempt < max_retries:
                    wait_time = retry_delay * (2 ** attempt)
                    logger.warning(
                        f"LLM API 频率限制（429），第 {attempt + 1}/{max_retries} 次重试，"
                        f"等待 {wait_time:.1f} 秒后重试..."
                    )
                    time.sleep(wait_time)
                    continue

                error_msg = f"LLM API 调用失败: {str(e)}"
                if is_rate_limit:
                    error_msg += f"（已重试 {attempt} 次）"
                raise Exception(error_msg) from e

        raise Exception("LLM API 调用失败：达到最大重试次数")

    def generate_stream(
        self,
        messages: List[Dict[str, Any]],
        max_retries: int = 3,
        initial_retry_delay: float = 1.0,
    ):
        if not messages:
            raise ValueError("messages 不能为空")

        retry_delay = initial_retry_delay

        for attempt in range(max_retries + 1):
            try:
                stream = self.client.chat.completions.create(
                    model=self.model,
                    messages=messages,
                    stream=True,
                    **self._chat_kwargs(),
                )
                for chunk in stream:
                    if chunk.choices and len(chunk.choices) > 0:
                        delta = chunk.choices[0].delta
                        if delta and delta.content:
                            yield delta.content
                return

            except Exception as e:
                err_body = getattr(e, "body", None)
                if err_body is not None:
                    logger.error("LLM API error body (stream): %s", err_body)
                resp = getattr(e, "response", None)
                if resp is not None:
                    try:
                        code = getattr(resp, "status_code", None)
                        txt = (getattr(resp, "text", None) or "")[:4000]
                        if txt or code:
                            logger.error(
                                "LLM HTTP 错误(stream) status=%s text(截断)=%s",
                                code,
                                txt,
                            )
                    except Exception:
                        pass

                if _is_billing_quota_error(e):
                    msg = (
                        "LLM API：账户额度已用尽（insufficient_quota）。详情: " + str(e)
                    )
                    logger.error(msg)
                    raise Exception(msg) from e

                is_rate_limit = False
                error_str = str(e).lower()
                if "429" in error_str or "rate_limit" in error_str or "too frequent" in error_str:
                    is_rate_limit = True
                if RateLimitError and isinstance(e, RateLimitError):
                    is_rate_limit = True

                if is_rate_limit and attempt < max_retries:
                    wait_time = retry_delay * (2 ** attempt)
                    logger.warning(
                        f"LLM API 频率限制（429），第 {attempt + 1}/{max_retries} 次重试，"
                        f"等待 {wait_time:.1f} 秒后重试..."
                    )
                    time.sleep(wait_time)
                    continue

                error_msg = f"LLM API 调用失败: {str(e)}"
                if is_rate_limit:
                    error_msg += f"（已重试 {attempt} 次）"
                raise Exception(error_msg) from e

        raise Exception("LLM API 调用失败：达到最大重试次数")
