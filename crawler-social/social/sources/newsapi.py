"""NewsAPI source (optional key)."""

from __future__ import annotations

import os
from typing import Any, Dict, List, Optional

import requests

from .base import BaseArticleSource

DEFAULT_EVERYTHING = "https://newsapi.org/v2/everything"


class NewsAPISource(BaseArticleSource):
    def __init__(
        self,
        api_key: Optional[str] = None,
        base_url: str = DEFAULT_EVERYTHING,
        language: Optional[str] = None,
        timeout: float = 20.0,
    ) -> None:
        self.api_key = (api_key or os.environ.get("NEWSAPI_KEY") or "").strip()
        self.base_url = base_url.rstrip("/")
        self.language = (language or os.environ.get("NEWSAPI_LANGUAGE") or "").strip() or None
        self.timeout = timeout

    def search(self, keyword: str, limit: int) -> List[Dict[str, Any]]:
        if not self.api_key:
            return []
        k = (keyword or "").strip()
        if not k:
            return []
        cap = max(1, min(limit, 20))
        params: Dict[str, Any] = {
            "q": k,
            "pageSize": cap,
            "sortBy": "publishedAt",
            "apiKey": self.api_key,
        }
        if self.language:
            params["language"] = self.language
        url = self.base_url
        if "/everything" not in url:
            url = url.rstrip("/") + "/everything"
        try:
            r = requests.get(url, params=params, timeout=self.timeout)
            r.raise_for_status()
            data = r.json()
        except Exception:
            return []
        if data.get("status") == "error":
            return []
        out: List[Dict[str, Any]] = []
        for a in data.get("articles") or []:
            if len(out) >= cap:
                break
            title = (a.get("title") or "").strip()
            article_url = (a.get("url") or "").strip()
            if not title or not article_url:
                continue
            summary = (a.get("description") or a.get("content") or "").strip() or None
            cover = (a.get("urlToImage") or "").strip() or None
            out.append(
                {
                    "title": title,
                    "summary": summary,
                    "coverUrl": cover,
                    "articleUrl": article_url,
                    "source": "newsapi",
                }
            )
        return out

