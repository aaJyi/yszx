# -*- coding: utf-8 -*-
"""
健康档案 /分析流水线调试日志。
仅输出摘要（条数、长度、片段），不打印完整 Base64 或整图 data URL，避免日志爆炸与泄露。
统一 logger名称：ai_agent.pipeline"""
from __future__ import annotations

import logging
from typing import Any, Dict, List, Optional

_LOG = logging.getLogger("ai_agent.pipeline")

TEXT_SNIP = 800
URL_PREFIX_LEN = 96


def log_raw_incoming(user_id: int, idx: int, rec: Dict[str, Any]) -> None:
    b64 = rec.get("rawData")
    n = len(b64) if isinstance(b64, str) else 0
    _LOG.info(
        "[PIPELINE] user=%s step=01_api_raw idx=%s rawId=%s dataType=%s formatType=%s "
        "base64StrLen=%s fileName=%s uploadTime=%s",
        user_id,
        idx,
        rec.get("id"),
        rec.get("dataType"),
        rec.get("formatType"),
        n,
        rec.get("fileName"),
        rec.get("uploadTime"),
    )


def log_after_decode(
    user_id: int,
    idx: int,
    raw_id: Any,
    data_type: str,
    format_type: str,
    decoded: Any,
) -> None:
    if isinstance(decoded, bytes):
        head = decoded[:16].hex() if len(decoded) >= 16 else decoded.hex()
        _LOG.info(
            "[PIPELINE] user=%s step=02_base64_decode idx=%s rawId=%s dataType=%s formatType=%s "
            "decodedBytes=%s headHex16=%s",
            user_id,
            idx,
            raw_id,
            data_type,
            format_type,
            len(decoded),
            head,
        )
    elif isinstance(decoded, str):
        snip = decoded[:TEXT_SNIP].replace("\n", "\\n")
        _LOG.info(
            "[PIPELINE] user=%s step=02_base64_decode idx=%s rawId=%s dataType=%s formatType=%s "
            "decodedTextLen=%s snippet=%s",
            user_id,
            idx,
            raw_id,
            data_type,
            format_type,
            len(decoded),
            snip,
        )
    elif isinstance(decoded, dict):
        keys = list(decoded.keys())[:24]
        _LOG.info(
            "[PIPELINE] user=%s step=02_base64_decode idx=%s rawId=%s dataType=%s formatType=%s "
            "decodedJson keys(first24)=%s",
            user_id,
            idx,
            raw_id,
            data_type,
            format_type,
            keys,
        )
    else:
        _LOG.info(
            "[PIPELINE] user=%s step=02_base64_decode idx=%s rawId=%s decodedType=%s repr=%s",
            user_id,
            idx,
            raw_id,
            type(decoded).__name__,
            str(decoded)[:300],
        )


def log_medical_contents(user_id: int, context: str, contents: List[Dict[str, Any]]) -> None:
    n_txt = sum(1 for c in contents if c.get("type") == "text")
    n_img = sum(1 for c in contents if c.get("type") == "image_url")
    text_chars = 0
    for c in contents:
        if c.get("type") == "text":
            text_chars += len(c.get("text") or "")
    _LOG.info(
        "[PIPELINE] user=%s step=03_build_contents ctx=%s parts=%s textParts=%s imageParts=%s totalTextChars=%s",
        user_id,
        context,
        len(contents),
        n_txt,
        n_img,
        text_chars,
    )
    for i, c in enumerate(contents):
        if c.get("type") == "text":
            t = c.get("text") or ""
            _LOG.info(
                "[PIPELINE] user=%s step=03_part ctx=%s i=%s type=text chars=%s head=%s",
                user_id,
                context,
                i,
                len(t),
                (t[:TEXT_SNIP].replace("\n", "\\n")),
            )
        elif c.get("type") == "image_url":
            url = (c.get("image_url") or {}).get("url") or ""
            _LOG.info(
                "[PIPELINE] user=%s step=03_part ctx=%s i=%s type=image_url urlLen=%s prefix=%s",
                user_id,
                context,
                i,
                len(url),
                url[:URL_PREFIX_LEN],
            )


def summarize_messages(messages: List[Dict[str, Any]]) -> str:
    segs = []
    for m in messages:
        role = m.get("role", "?")
        content = m.get("content")
        if isinstance(content, str):
            segs.append(f"{role}:str_len={len(content)}")
        elif isinstance(content, list):
            n_txt = sum(
                1 for x in content if isinstance(x, dict) and str(x.get("type", "")).lower() == "text"
            )
            n_img = sum(
                1
                for x in content
                if isinstance(x, dict) and str(x.get("type", "")).lower() == "image_url"
            )
            chars = 0
            for x in content:
                if isinstance(x, dict) and str(x.get("type", "")).lower() == "text":
                    chars += len(str(x.get("text", "")))
            segs.append(f"{role}:blocks={len(content)} text={n_txt} img={n_img} textChars={chars}")
        else:
            segs.append(f"{role}:type={type(content).__name__}")
    return " | ".join(segs)


def log_llm_request(user_id: Optional[int], label: str, messages: List[Dict[str, Any]]) -> None:
    _LOG.info(
        "[PIPELINE] user=%s step=04_llm_request label=%s summary=%s",
        user_id,
        label,
        summarize_messages(messages),
    )


def log_llm_response(
    user_id: Optional[int],
    label: str,
    elapsed_ms: float,
    text: Optional[str],
) -> None:
    t = text or ""
    _LOG.info(
        "[PIPELINE] user=%s step=05_llm_response label=%s elapsedMs=%.0f outChars=%s head=%s",
        user_id,
        label,
        elapsed_ms,
        len(t),
        t[:500].replace("\n", "\\n"),
    )


def log_archive_json_summary(user_id: int, context: str, archive: Dict[str, Any]) -> None:
    try:
        keys = list(archive.keys())[:30]
    except Exception:
        keys = []
    _LOG.info(
        "[PIPELINE] user=%s step=06_archive_json ctx=%s topKeys=%s",
        user_id,
        context,
        keys,
    )


def log_analysis_input_sizes(user_id: int, archive_json_len: int, raw_text_len: int, n_images: int) -> None:
    _LOG.info(
        "[PIPELINE] user=%s step=07_analysis_input archiveJsonChars=%s rawDataTextChars=%s imageCount=%s",
        user_id,
        archive_json_len,
        raw_text_len,
        n_images,
    )
