"""Source abstraction."""

from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Any, Dict, List


class BaseArticleSource(ABC):
    @abstractmethod
    def search(self, keyword: str, limit: int) -> List[Dict[str, Any]]:
        raise NotImplementedError

