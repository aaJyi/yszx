from __future__ import annotations

import re
import time
from typing import Any

import requests
from bs4 import BeautifulSoup

from app.config import settings


class SearchError(RuntimeError):
    pass


def _search_duckduckgo_html(query: str, max_results: int, timeout_secs: int, headers: dict[str, str]) -> list[dict[str, Any]]:
    url = "https://duckduckgo.com/html/"
    r = requests.get(url, params={"q": query}, headers=headers, timeout=timeout_secs)
    if r.status_code != 200:
        raise SearchError(f"DuckDuckGo failed: {r.status_code}")

    soup = BeautifulSoup(r.text, "lxml")
    results: list[dict[str, Any]] = []

    for a in soup.select("a.result__a"):
        title = a.get_text(strip=True)
        href = a.get("href") or ""
        href = re.sub(r"\s+", "", href)
        if not href:
            continue
        results.append({"title": title, "url": href})
        if len(results) >= max_results:
            break
    return results


def _search_bing_html(query: str, max_results: int, timeout_secs: int, headers: dict[str, str]) -> list[dict[str, Any]]:
    # 作为 DuckDuckGo 的无 key 兜底检索通道
    url = "https://www.bing.com/search"
    r = requests.get(url, params={"q": query}, headers=headers, timeout=timeout_secs)
    if r.status_code != 200:
        raise SearchError(f"Bing failed: {r.status_code}")

    soup = BeautifulSoup(r.text, "lxml")
    results: list[dict[str, Any]] = []

    for a in soup.select("li.b_algo h2 a"):
        title = a.get_text(strip=True)
        href = a.get("href") or ""
        href = re.sub(r"\s+", "", href)
        if not href:
            continue
        results.append({"title": title, "url": href})
        if len(results) >= max_results:
            break
    return results


def web_search_duckduckgo(query: str, max_results: int = 5) -> list[dict[str, Any]]:
    """
    联网检索（DuckDuckGo + Bing fallback）。
    - DuckDuckGo: 最多重试 3 次（指数退避）
    - Bing: 作为兜底，最多重试 2 次
    """
    headers = {"User-Agent": settings.search_user_agent}
    timeout_secs = settings.search_timeout_secs

    last_err: Exception | None = None

    # 主通道：DuckDuckGo（重试）
    for attempt in range(3):
        try:
            results = _search_duckduckgo_html(query, max_results, timeout_secs, headers)
            if results:
                return results
            last_err = SearchError("DuckDuckGo returned empty results")
        except Exception as e:  # noqa: BLE001
            last_err = e
        time.sleep(2 ** attempt)

    # 兜底通道：Bing（重试）
    for attempt in range(2):
        try:
            results = _search_bing_html(query, max_results, timeout_secs, headers)
            if results:
                return results
            last_err = SearchError("Bing returned empty results")
        except Exception as e:  # noqa: BLE001
            last_err = e
        time.sleep(1 + attempt)

    raise SearchError(f"Search failed after retries and fallback: {last_err}")

