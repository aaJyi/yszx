import json
from typing import Any

import requests

from app.config import settings


class LLMError(RuntimeError):
    pass


def ollama_chat_json(system: str, user: str, schema_hint: str | None = None) -> Any:
    """
    调用 Ollama chat 接口，尽量返回 JSON（失败则抛错）。
    """
    url = f"{settings.ollama_base_url.rstrip('/')}/api/chat"
    prompt = user if not schema_hint else f"{user}\n\nJSON约束：\n{schema_hint}"
    payload = {
        "model": settings.ollama_model,
        "stream": False,
        "messages": [
            {"role": "system", "content": system},
            {"role": "user", "content": prompt},
        ],
        "options": {
            "temperature": 0.2,
        },
    }
    r = requests.post(url, json=payload, timeout=120)
    if r.status_code != 200:
        raise LLMError(f"Ollama error: {r.status_code} {r.text}")
    data = r.json()
    content = data.get("message", {}).get("content", "")
    try:
        return json.loads(content)
    except Exception as e:
        raise LLMError(f"LLM did not return valid JSON. content={content[:500]}") from e


def ollama_chat_text(system: str, user: str) -> str:
    url = f"{settings.ollama_base_url.rstrip('/')}/api/chat"
    payload = {
        "model": settings.ollama_model,
        "stream": False,
        "messages": [
            {"role": "system", "content": system},
            {"role": "user", "content": user},
        ],
        "options": {
            "temperature": 0.4,
        },
    }
    r = requests.post(url, json=payload, timeout=180)
    if r.status_code != 200:
        raise LLMError(f"Ollama error: {r.status_code} {r.text}")
    data = r.json()
    return data.get("message", {}).get("content", "")

