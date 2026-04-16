"""
Health article HTTP service for Java backend calls.

Start:
  python -m uvicorn social.http_api:app --host 127.0.0.1 --port 8095
"""

from __future__ import annotations

import os
from typing import Any, Dict, List

from fastapi import FastAPI, Query
from fastapi.middleware.cors import CORSMiddleware

from .aggregator import Aggregator
from .sources.google_news_rss import GoogleNewsRSSSource
from .sources.newsapi import NewsAPISource
from .sources.rss import RSSSource

app = FastAPI(title="happyLife health articles", version="1.0.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


def _build_aggregator() -> Aggregator:
    return Aggregator([GoogleNewsRSSSource(), NewsAPISource(), RSSSource()])


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "service": "health-articles"}


@app.get("/articles")
def articles(
    keyword: str = Query(..., min_length=1, description="query keyword"),
    limit: int = Query(8, ge=1, le=30, description="max result size"),
) -> List[Dict[str, Any]]:
    agg = _build_aggregator()
    return agg.search(keyword.strip(), limit)


def main() -> None:
    import uvicorn

    host = os.environ.get("HEALTH_ARTICLE_API_HOST", "127.0.0.1")
    port = int(os.environ.get("HEALTH_ARTICLE_API_PORT", "8095"))
    uvicorn.run(app, host=host, port=port)


if __name__ == "__main__":
    main()

