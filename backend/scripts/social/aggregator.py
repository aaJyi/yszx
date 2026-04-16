"""多源聚合与 URL 去重。"""

from __future__ import annotations

from typing import Any, Dict, List

from .sources.base import BaseArticleSource


def dedupe_by_url(items: List[Dict[str, Any]], url_key: str = "articleUrl") -> List[Dict[str, Any]]:
    seen: set[str] = set()
    out: List[Dict[str, Any]] = []
    for it in items:
        u = (it.get(url_key) or "").strip()
        if not u or u in seen:
            continue
        seen.add(u)
        out.append(it)
    return out


class Aggregator:
    def __init__(self, sources: List[BaseArticleSource]) -> None:
        self.sources = sources

    def search(self, keyword: str, limit: int) -> List[Dict[str, Any]]:
        cap = max(1, min(limit, 50))
        merged: List[Dict[str, Any]] = []
        for src in self.sources:
            if len(merged) >= cap:
                break
            try:
                part = src.search(keyword, cap - len(merged))
            except Exception:
                part = []
            merged.extend(part)
        merged = dedupe_by_url(merged)[:cap]
        return merged
