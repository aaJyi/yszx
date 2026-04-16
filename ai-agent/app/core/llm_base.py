"""
LLM 基础抽象类
"""
from abc import ABC, abstractmethod
from typing import Optional


class BaseLLM(ABC):
    """LLM 基础接口"""
    
    @abstractmethod
    async def generate(self, prompt: str) -> str:
        """
        生成文本
        
        Args:
            prompt: 输入提示词
            
        Returns:
            模型生成的文本
        """
        pass
