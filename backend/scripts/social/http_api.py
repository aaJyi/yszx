"""
健康资讯 HTTP 服务：供 Java 后端调用，内部使用与 `python -m social.cli` 相同的 RSS 聚合（Google News RSS 等），无需 NewsAPI key。

启动（在 backend/scripts 目录下，已安装 social/requirements.txt）：

  pip install -r social/requirements.txt
  python -m uvicorn social.http_api:app --host 127.0.0.1 --port 8095

Java 配置：health.news.python-service.base-url=http://127.0.0.1:8095
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
    # 与 cli 一致：RSS 为主；NewsAPI 仅当环境变量有 key 时才有数据
    return Aggregator(
        [
            GoogleNewsRSSSource(),
            NewsAPISource(),
            RSSSource(),
        ]
    )


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "service": "health-articles"}


@app.get("/articles")
def articles(
    keyword: str = Query(..., min_length=1, description="检索关键词，如疾病名"),
    limit: int = Query(8, ge=1, le=30, description="条数上限"),
) -> List[Dict[str, Any]]:
    """返回与 CLI --json 相同结构的列表（title/summary/coverUrl/articleUrl/source）。"""
    agg = _build_aggregator()
    rows = agg.search(keyword.strip(), limit)
    return rows


def main() -> None:
    import uvicorn

    host = os.environ.get("HEALTH_ARTICLE_API_HOST", "127.0.0.1")
    port = int(os.environ.get("HEALTH_ARTICLE_API_PORT", "8095"))
    uvicorn.run(app, host=host, port=port)


if __name__ == "__main__":
    main()
