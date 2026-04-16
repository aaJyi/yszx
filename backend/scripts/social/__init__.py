"""
健康资讯多数据源（NewsAPI、RSS 等），不再对接今日头条检索接口。

命令行：在 backend/scripts 下执行  python -m social.cli -k 关键词 --json
"""

from .aggregator import Aggregator, dedupe_by_url

__all__ = ["Aggregator", "dedupe_by_url"]
