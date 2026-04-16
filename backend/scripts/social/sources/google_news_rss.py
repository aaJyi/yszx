"""
Google News 关键词 RSS（官方 XML 流，非网页 HTML 逆向）。

RSS 里的 link 常为 news.google.com/rss/articles/... 中转链，直接打开易空白；
默认跟随 HTTP 重定向解析为媒体原文 URL（与后端 health.news.google-rss.resolve-redirects 一致）。

若解析后仍是境外站点（如 bbc.com），在中国大陆网络下可能 ERR_FAILED，属网络策略，非程序错误。

示例 URL：
  https://news.google.com/rss/search?q=糖尿病&hl=zh-CN&gl=CN&ceid=CN:zh-Hans
"""

from __future__ import annotations

import html
import os
import re
import urllib.parse
from typing import Any, Dict, List

from .base import BaseArticleSource

GOOGLE_NEWS_RSS_SEARCH = "https://news.google.com/rss/search"


def _resolve_final_url(url: str, timeout: float = 15.0) -> str:
    """将 Google News 中转链跟随重定向为原文链接。"""
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
    """去标签、解 HTML 实体（如 &nbsp;）、合并空白。"""
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
            if not link:
                continue
            if not title:
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
