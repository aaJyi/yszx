"""RSS/Atom：通过 feedparser 拉取，按关键词过滤标题/摘要。"""

from __future__ import annotations

import os
from typing import Any, Dict, List, Optional

from .base import BaseArticleSource


def _contains_kw(text: str, keyword: str) -> bool:
    if not text or not keyword:
        return False
    return keyword.lower() in text.lower()


class RSSSource(BaseArticleSource):
    def __init__(self, feed_urls: Optional[List[str]] = None) -> None:
        raw = os.environ.get("RSS_FEED_URLS", "")
        env_urls = [u.strip() for u in raw.split(",") if u.strip()]
        self.feed_urls = feed_urls if feed_urls is not None else env_urls

    def search(self, keyword: str, limit: int) -> List[Dict[str, Any]]:
        try:
            import feedparser
        except ImportError:
            return []
        k = (keyword or "").strip()
        if not k or not self.feed_urls:
            return []
        cap = max(1, min(limit, 30))
        out: List[Dict[str, Any]] = []
        for feed_url in self.feed_urls:
            if len(out) >= cap:
                break
            try:
                feed = feedparser.parse(feed_url)
            except Exception:
                continue
            for entry in feed.entries or []:
                if len(out) >= cap:
                    break
                title = getattr(entry, "title", "") or ""
                summary = (
                    getattr(entry, "summary", "")
                    or getattr(entry, "description", "")
                    or ""
                )
                if not _contains_kw(title, k) and not _contains_kw(summary, k):
                    continue
                link = getattr(entry, "link", "") or ""
                if not link:
                    continue
                out.append(
                    {
                        "title": title.strip(),
                        "summary": summary[:2000] if summary else None,
                        "coverUrl": None,
                        "articleUrl": link.strip(),
                        "source": "rss",
                    }
                )
        return out
