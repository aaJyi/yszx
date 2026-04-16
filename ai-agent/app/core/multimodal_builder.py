"""
多模态消息构建器
用于构建 OpenAI / Qwen-VL 兼容的多模态消息结构
"""
import logging
from typing import Optional
from typing import List, Dict, Any

# 配置日志
logger = logging.getLogger(__name__)


def _extract_content(item: Dict[str, Any]) -> tuple[str, Optional[str]]:
    """
    兼容两种内容结构：
    1) 老结构：{"type":"text|image", "data":"..."}
    2) 新结构：{"type":"text","text":"..."} / {"type":"image_url","image_url":{"url":"..."}}
    """
    content_type = str(item.get("type", "")).lower()

    # 老结构
    if "data" in item and item.get("data") is not None:
        return content_type, str(item.get("data"))

    # 新结构：text
    if content_type == "text" and item.get("text") is not None:
        return "text", str(item.get("text"))

    # 新结构：image_url
    if content_type == "image_url":
        image_url = item.get("image_url")
        if isinstance(image_url, dict) and image_url.get("url"):
            return "image", str(image_url.get("url"))

    return content_type, None


def build_medical_messages(system_prompt: str, contents: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """
    构建医疗咨询的多模态消息结构
    
    Args:
        system_prompt: 系统提示词
        contents: 内容列表，每一项是包含 type 和 data 的字典
            示例:
            [
                {"type": "text", "data": "血常规：白细胞偏高"},
                {"type": "image", "data": "https://example.com/image.jpg"},
                {"type": "image", "data": "iVBORw0KGgoAAAANSUhEUgAA..."}  # base64
            ]
    
    Returns:
        OpenAI / Qwen-VL 等兼容的 messages 列表
        格式:
        [
            {"role": "user", "content": [
                {"type": "text", "text": system_prompt},  # system_prompt 合并到 user 消息中
                {"type": "text", "text": "..."},
                {"type": "image_url", "image_url": {"url": "..."}},
                ...
            ]}
        ]
        注意：部分兼容接口（如 Ollama）只支持 user 和 assistant，不支持独立 system 角色
    
    Raises:
        ValueError: 当 contents 格式不正确时
    """
    if not isinstance(contents, list):
        raise ValueError("contents 必须是列表类型")
    
    # 构建 messages 列表
    messages = []
    
    # 注意：Ollama 等兼容接口常将 system 合并进 user
    # 将 system_prompt 合并到 user 消息的开头
    
    # 构建 user message 的 content 列表
    user_content = []
    
    # 如果有 system_prompt，将其作为第一条文本内容
    if system_prompt:
        user_content.append({
            "type": "text",
            "text": system_prompt
        })
        logger.debug(f"添加 system 提示词到 user 消息，长度: {len(system_prompt)} 字符")
    
    for idx, item in enumerate(contents):
        if not isinstance(item, dict):
            raise ValueError(f"contents[{idx}] 必须是字典类型")
        
        content_type, content_data = _extract_content(item)
        
        if content_data is None:
            logger.warning(f"contents[{idx}] 的有效内容为空，跳过")
            continue
        
        if content_type == "text":
            # 文本类型
            user_content.append({
                "type": "text",
                "text": str(content_data)
            })
            logger.debug(f"添加文本内容，长度: {len(str(content_data))} 字符")
            
        elif content_type == "image":
            # 图片类型：判断是 URL、data URL 还是 base64
            image_data = str(content_data)
            
            # 判断是否为 HTTP/HTTPS URL
            if image_data.startswith(("http://", "https://")):
                # URL 格式
                user_content.append({
                    "type": "image_url",
                    "image_url": {
                        "url": image_data
                    }
                })
                logger.debug(f"添加图片 URL: {image_data[:50]}...")
            elif image_data.startswith("data:"):
                # 已经是 data URL 格式，直接使用
                user_content.append({
                    "type": "image_url",
                    "image_url": {
                        "url": image_data
                    }
                })
                logger.debug(f"添加 data URL 图片，数据长度: {len(image_data)} 字符")
            else:
                # Base64 格式：转换为 data URL
                # 假设是 PNG 格式（如果需要支持其他格式，可以扩展）
                data_url = f"data:image/png;base64,{image_data}"
                user_content.append({
                    "type": "image_url",
                    "image_url": {
                        "url": data_url
                    }
                })
                logger.debug(f"添加 Base64 图片，数据长度: {len(image_data)} 字符")
        else:
            # 未知类型，记录警告但继续处理
            logger.warning(f"未知的内容类型: {content_type}，跳过")
            continue
    
    # 添加 user message
    if user_content:
        messages.append({
            "role": "user",
            "content": user_content
        })
        logger.debug(f"添加 user 消息，包含 {len(user_content)} 个内容项")
    else:
        logger.warning("user_content 为空，未添加 user 消息")
    
    logger.info(f"消息构建完成，共 {len(messages)} 条消息")
    return messages
