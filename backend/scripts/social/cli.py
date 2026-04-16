#!/usr/bin/env python3
"""
健康资讯多源检索 CLI，与后端 Java HealthArticleFetchServiceImpl 一致：
  1) Google News RSS（关键词，标准 XML，主力）
  2) NewsAPI（可选，NEWSAPI_KEY）
  3) 静态 RSS 列表（RSS_FEED_URLS）

在 backend/scripts 下：
  pip install -r social/requirements.txt
  python -m social.cli -k 糖尿病 --limit 8 --json

环境变量：
  NEWSAPI_KEY       NewsAPI key（可选）
  NEWSAPI_LANGUAGE  可选 zh / en
  RSS_FEED_URLS     逗号分隔多个 RSS 地址（可选）
"""

from __future__ import annotations

import argparse
import json
import sys
import urllib.parse

from .aggregator import Aggregator
from .sources.google_news_rss import GoogleNewsRSSSource
from .sources.newsapi import NewsAPISource
from .sources.rss import RSSSource


def main() -> int:
    p = argparse.ArgumentParser(description="健康资讯多源检索（NewsAPI + RSS）")
    p.add_argument("-k", "--keyword", required=True, help="关键词（如疾病名）")
    p.add_argument("-n", "--limit", type=int, default=8, help="条数上限")
    p.add_argument("--json", action="store_true", help="输出 JSON")
    args = p.parse_args()

    sources = [
        GoogleNewsRSSSource(),
        NewsAPISource(),
        RSSSource(),
    ]
    agg = Aggregator(sources)
    rows = agg.search(args.keyword, args.limit)

    if not rows:
        # --json 时输出 []，与后端知识库「只存真实 RSS 条、不写搜索占位」一致；非 JSON 仍给一条人工可点的提示
        if args.json:
            rows = []
        else:
            q = args.keyword
            rows = [
                {
                    "title": f"在 Google 新闻中搜索「{q}」",
                    "summary": "Google News RSS 与各源均无条目时，请检查网络或使用下方聚合搜索。",
                    "coverUrl": None,
                    "articleUrl": "https://news.google.com/search?q="
                    + urllib.parse.quote(q)
                    + "&hl=zh-CN&gl=CN&ceid=CN:zh-Hans",
                    "source": "fallback",
                }
            ]

    if args.json:
        print(json.dumps(rows, ensure_ascii=False, indent=2))
    else:
        for i, r in enumerate(rows, 1):
            print(f"{i}. {r.get('title')}")
            print(f"   {r.get('articleUrl')}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
