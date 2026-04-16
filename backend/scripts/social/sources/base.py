"""多数据源抽象：实现 search(keyword, limit) -> 统一字典列表。"""

from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Any, Dict, List


class BaseArticleSource(ABC):
    @abstractmethod
    def search(self, keyword: str, limit: int) -> List[Dict[str, Any]]:
        """
        返回统一结构：
        title, summary, coverUrl, articleUrl
        """
        raise NotImplementedError
