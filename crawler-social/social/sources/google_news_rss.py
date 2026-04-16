"""Google News RSS source."""

from __future__ import annotations

import html
import os
import re
import urllib.parse
from typing import Any, Dict, List

from .base import BaseArticleSource

GOOGLE_NEWS_RSS_SEARCH = "https://news.google.com/rss/search"


def _resolve_final_url(url: str, timeout: float = 15.0) -> str:
    if "news.google.com" not in url:
        return url
    if os.environ.get("GOOGLE_RSS_RESOLVE_REDIRECTS", "1").strip() in ("0", "false", "no"):
        return url
    try:
        import requests

        r = requests.get(
            url,
            allow_redirects=True,
            timeout=timeout,
            headers={
                "User-Agent": (
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                )
            },
            stream=True,
        )
        try:
            return (r.url or url).strip()
        finally:
            r.close()
    except Exception:
        return url


def _strip_html(s: str) -> str:
    if not s:
        return ""
    t = re.sub(r"<[^>]+>", "", s)
    t = html.unescape(t)
    t = re.sub(r"\s+", " ", t).strip()
    return t


class GoogleNewsRSSSource(BaseArticleSource):
    def search(self, keyword: str, limit: int) -> List[Dict[str, Any]]:
        try:
            import feedparser
        except ImportError:
            return []
        k = (keyword or "").strip()
        if not k:
            return []
        cap = max(1, min(limit, 30))
        q = urllib.parse.urlencode(
            {
                "q": k,
                "hl": "zh-CN",
                "gl": "CN",
                "ceid": "CN:zh-Hans",
            }
        )
        url = f"{GOOGLE_NEWS_RSS_SEARCH}?{q}"
        feed = feedparser.parse(url)
        out: List[Dict[str, Any]] = []
        for entry in feed.entries or []:
            if len(out) >= cap:
                break
            title = _strip_html(str(getattr(entry, "title", None) or ""))
            link = (getattr(entry, "link", None) or "").strip()
            if not link or not title:
                continue
            raw_sum = getattr(entry, "summary", None) or getattr(entry, "description", None) or ""
            summary = _strip_html(str(raw_sum))[:2000] or None
            article_url = _resolve_final_url(link)
            out.append(
                {
                    "title": title,
                    "summary": summary,
                    "coverUrl": None,
                    "articleUrl": article_url,
                    "source": "google_news_rss",
                }
            )
        return out

