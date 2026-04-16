"""
Qwen3-VL API Adapter（多模态视觉语言模型）
"""
import os
from typing import List, Dict, Any, Optional

try:
    from openai import OpenAI
    HAS_OPENAI = True
except ImportError:
    HAS_OPENAI = False


class QwenVLAdapter:
    """Qwen3-VL API 适配器（支持多模态输入）"""
    
    def __init__(
        self,
        api_key: Optional[str] = None,
        base_url: str = "https://dashscope.aliyuncs.com/compatible-mode/v1",
        model: str = "qwen3-vl-flash"
    ):
        """
        初始化 Qwen3-VL 适配器
        
        Args:
            api_key: API Key（默认从环境变量 DASHSCOPE_API_KEY 读取）
            base_url: API Base URL
            model: 模型名称（如 qwen3-vl-flash, qwen3-vl-plus）
        """
        if not HAS_OPENAI:
            raise ImportError("请安装 openai 库: pip install openai")
        
        # 从环境变量或参数获取 API Key
        api_key = api_key or os.getenv("DASHSCOPE_API_KEY")
        if not api_key:
            raise ValueError("API Key 未设置，请设置环境变量 DASHSCOPE_API_KEY 或传入 api_key 参数")
        
        # 初始化 OpenAI 客户端（兼容模式）
        self.client = OpenAI(
            api_key=api_key,
            base_url=base_url
        )
        self.model = model
    
    def generate(self, messages: List[Dict[str, Any]]) -> str:
        """
        生成文本（支持多模态输入）
        
        Args:
            messages: 消息列表，支持 OpenAI 标准格式
                示例:
                    [
                        {"role": "user", "content": [
                            {"type": "image_url", "image_url": {"url": "https://example.com/image.jpg"}},
                            {"type": "text", "text": "图中描绘的是什么景象?"}
                        ]}
                    ]
        
        Returns:
            模型生成的文本
        
        Raises:
            ValueError: 当 messages 格式不正确时
            Exception: 当 API 调用失败时
        """
        if not messages:
            raise ValueError("messages 不能为空")
        
        try:
            completion = self.client.chat.completions.create(
                model=self.model,
                messages=messages
            )
            
            return completion.choices[0].message.content
            
        except Exception as e:
            error_msg = f"Qwen3-VL API 调用失败: {str(e)}"
            raise Exception(error_msg) from e
    
    def generate_stream(self, messages: List[Dict[str, Any]]):
        """
        流式生成文本（支持多模态输入）
        
        Args:
            messages: 消息列表，支持 OpenAI 标准格式
        
        Yields:
            模型生成的文本片段（逐token）
        
        Raises:
            ValueError: 当 messages 格式不正确时
            Exception: 当 API 调用失败时
        """
        if not messages:
            raise ValueError("messages 不能为空")
        
        try:
            # 使用 stream=True 启用流式输出
            stream = self.client.chat.completions.create(
                model=self.model,
                messages=messages,
                stream=True
            )
            
            # 逐块返回内容
            for chunk in stream:
                if chunk.choices and len(chunk.choices) > 0:
                    delta = chunk.choices[0].delta
                    if delta and delta.content:
                        yield delta.content
                        
        except Exception as e:
            error_msg = f"Qwen3-VL API 调用失败: {str(e)}"
            raise Exception(error_msg) from e