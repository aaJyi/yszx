"""
医疗多模态内容构建器
用于将健康数据记录转换为多模态消息内容
"""
import json
import logging
import os
from typing import List, Dict, Any

from app.core.pdf_to_image import pdf_bytes_to_images
from app.core.image_utils import image_bytes_to_llm_data_url
from app.core.config import settings

# 配置日志
logger = logging.getLogger(__name__)
MAX_PDF_PAGES = max(1, int(os.getenv("AI_AGENT_MAX_PDF_PAGES", "12")))


def build_medical_contents(records: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """
    将健康数据记录转换为多模态消息内容
    
    Args:
        records: 健康数据记录列表，每条记录包含：
            - dataType: 数据类型（如 REPORT, MEDICAL_RECORD 等）
            - formatType: 格式类型（TEXT, JSON, IMAGE, PDF 等）
            - decoded_data: 解码后的数据（bytes / str / dict）
            示例:
            [
                {
                    "dataType": "REPORT",
                    "formatType": "TEXT",
                    "decoded_data": "血常规：白细胞偏高"
                },
                {
                    "dataType": "REPORT",
                    "formatType": "IMAGE",
                    "decoded_data": b'\x89PNG\r\n\x1a\n...'  # bytes
                },
                {
                    "dataType": "MEDICAL_RECORD",
                    "formatType": "PDF",
                    "decoded_data": b'%PDF-1.4...'  # bytes
                }
            ]
    
    Returns:
        多模态内容列表，格式为 OpenAI / Qwen-VL 兼容格式
        示例:
        [
            {"type": "text", "text": "血常规：白细胞偏高"},
            {"type": "image_url", "image_url": {"url": "data:image/png;base64,..."}},
            {"type": "image_url", "image_url": {"url": "data:image/png;base64,..."}},  # PDF 第1页
            {"type": "image_url", "image_url": {"url": "data:image/png;base64,..."}}   # PDF 第2页
        ]
    
    Raises:
        ValueError: 当 records 格式不正确时
        RuntimeError: 当处理过程中发生错误时
    """
    if not isinstance(records, list):
        raise ValueError("records 必须是列表类型")
    
    contents = []
    
    for idx, record in enumerate(records):
        if not isinstance(record, dict):
            raise ValueError(f"records[{idx}] 必须是字典类型")
        
        format_type = record.get("formatType", "").upper()
        decoded_data = record.get("decoded_data")
        data_type = record.get("dataType", "UNKNOWN")
        
        # 跳过没有 decoded_data 的记录
        if decoded_data is None:
            logger.warning(f"records[{idx}] 的 decoded_data 为空，跳过")
            continue
        
        try:
            if format_type in ("TEXT", "JSON"):
                # TEXT / JSON：转为文本内容
                if format_type == "JSON":
                    # JSON 格式：转换为格式化的 JSON 字符串
                    if isinstance(decoded_data, dict):
                        text_content = json.dumps(decoded_data, ensure_ascii=False, indent=2)
                    else:
                        # 如果不是 dict，直接转为字符串
                        text_content = str(decoded_data)
                else:
                    # TEXT 格式：直接使用字符串
                    text_content = str(decoded_data)
                
                contents.append({
                    "type": "text",
                    "text": text_content
                })
                logger.debug(f"添加文本内容（{format_type}），长度: {len(text_content)} 字符")
                
            elif format_type == "IMAGE":
                # IMAGE：bytes → base64 image_url
                if not isinstance(decoded_data, bytes):
                    logger.warning(f"records[{idx}] IMAGE 格式的 decoded_data 不是 bytes 类型，跳过")
                    continue
                
                data_url = image_bytes_to_llm_data_url(
                    decoded_data, max_long_edge=settings.llm_image_max_long_edge
                )
                
                contents.append({
                    "type": "image_url",
                    "image_url": {
                        "url": data_url
                    }
                })
                logger.debug(f"添加图片内容，原始大小: {len(decoded_data)} 字节")
                
            elif format_type == "PDF":
                # PDF：PDF bytes → 多张图片 → 多个 image_url
                if not isinstance(decoded_data, bytes):
                    logger.warning(f"records[{idx}] PDF 格式的 decoded_data 不是 bytes 类型，跳过")
                    continue
                
                try:
                    # 将 PDF 转换为多张图片
                    image_bytes_list = pdf_bytes_to_images(decoded_data)
                    
                    # 限制入模页数，避免超长多模态输入导致模型输出漂移
                    limited_images = image_bytes_list[:MAX_PDF_PAGES]
                    if len(image_bytes_list) > MAX_PDF_PAGES:
                        logger.warning(
                            "PDF 页数 %s 超过上限 %s，仅取前 %s 页参与建档",
                            len(image_bytes_list), MAX_PDF_PAGES, MAX_PDF_PAGES
                        )

                    # 为每一页图片创建 image_url
                    for page_idx, image_bytes in enumerate(limited_images):
                        data_url = image_bytes_to_llm_data_url(
                            image_bytes, max_long_edge=settings.llm_image_max_long_edge
                        )
                        
                        contents.append({
                            "type": "image_url",
                            "image_url": {
                                "url": data_url
                            }
                        })
                        logger.debug(f"添加 PDF 第 {page_idx + 1} 页图片，大小: {len(image_bytes)} 字节")
                    
                    logger.info(f"PDF 转换完成，共 {len(image_bytes_list)} 页，入模 {len(limited_images)} 页")
                    
                except Exception as e:
                    error_msg = f"处理 PDF 记录时出错: {str(e)}"
                    logger.error(error_msg)
                    # 继续处理其他记录，不中断整个流程
                    continue
                    
            else:
                # 未知格式，记录警告但继续处理
                logger.warning(f"未知的 formatType: {format_type}，跳过 records[{idx}]")
                continue
                
        except Exception as e:
            error_msg = f"处理 records[{idx}] 时发生错误: {str(e)}"
            logger.error(error_msg, exc_info=True)
            # 继续处理其他记录，不中断整个流程
            continue
    
    logger.info(f"内容构建完成，共 {len(contents)} 个内容项")
    return contents
