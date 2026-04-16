"""
LLM 接口和实现
"""
import asyncio
from abc import ABC, abstractmethod
from typing import List, Dict, Any, Optional
import time
import random


class BaseLLM(ABC):
    """LLM 基础接口"""
    
    @abstractmethod
    async def chat(self, messages: List[Dict[str, str]], **kwargs) -> str:
        """
        聊天接口
        
        Args:
            messages: 消息列表，格式 [{"role": "user", "content": "..."}]
            **kwargs: 其他参数
            
        Returns:
            模型回复文本
        """
        pass
    
    @abstractmethod
    async def stream_chat(self, messages: List[Dict[str, str]], **kwargs):
        """
        流式聊天接口（生成器）
        
        Args:
            messages: 消息列表
            **kwargs: 其他参数
            
        Yields:
            模型回复的文本片段
        """
        pass


class FakeLLM(BaseLLM):
    """假 LLM 实现，用于占位和测试"""
    
    def __init__(self, delay: float = 0.5):
        """
        Args:
            delay: 模拟延迟时间（秒）
        """
        self.delay = delay
        self.responses = [
            "根据您描述的症状，建议您注意休息，多喝水。如果症状持续，请及时就医。",
            "这种情况可能由多种原因引起。建议您详细描述症状，以便更准确地判断。",
            "医疗建议仅供参考，如有不适请及时就医。您可以尝试调整作息，保持良好心态。",
            "根据您的情况，建议您关注以下几点：1. 注意休息 2. 合理饮食 3. 适当运动 4. 定期检查。",
            "感谢您的咨询。建议您咨询专业医生以获得更准确的诊断和治疗建议。"
        ]
    
    async def chat(self, messages: List[Dict[str, str]], **kwargs) -> str:
        """模拟聊天响应"""
        await asyncio.sleep(self.delay)
        
        # 获取最后一条用户消息
        user_message = ""
        for msg in reversed(messages):
            if msg.get("role") == "user":
                user_message = msg.get("content", "")
                break
        
        # 根据用户消息生成简单响应
        response = random.choice(self.responses)
        
        # 如果用户消息包含关键词，返回更相关的回复
        if "头痛" in user_message or "头疼" in user_message:
            response = "头痛可能由多种原因引起，如疲劳、压力、睡眠不足等。建议您先休息，如果持续或加重，请就医。"
        elif "发烧" in user_message or "发热" in user_message:
            response = "发热是身体对疾病的反应。建议测量体温，多休息多喝水。如果体温超过38.5度或持续不退，请就医。"
        elif "咳嗽" in user_message:
            response = "咳嗽可能是呼吸道问题的表现。建议多喝温水，避免刺激性食物。如症状持续或加重，请咨询医生。"
        
        return response
    
    async def stream_chat(self, messages: List[Dict[str, str]], **kwargs):
        """模拟流式聊天响应"""
        full_response = await self.chat(messages, **kwargs)
        
        # 模拟流式输出，逐字返回
        words = full_response.split()
        for i, word in enumerate(words):
            await asyncio.sleep(0.1)
            yield word + (" " if i < len(words) - 1 else "")
