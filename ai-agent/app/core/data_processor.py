"""
健康数据处理器
用于解码健康数据中的 rawData 字段
"""
import base64
import json
import logging
from typing import Any, Dict, Optional

from app.core.pipeline_trace import log_after_decode

# 配置日志
logger = logging.getLogger(__name__)


class DataProcessor:
    """健康数据处理器"""
    
    # 返回 bytes 的格式类型
    BINARY_FORMATS = {"IMAGE", "PDF", "CSV"}
    
    # 返回 dict 的格式类型
    JSON_FORMAT = "JSON"
    
    # 返回 str 的格式类型
    TEXT_FORMAT = "TEXT"
    
    def decode_raw_data(
        self,
        record: Dict[str, Any],
        *,
        pipeline_user_id: Optional[int] = None,
        pipeline_idx: int = 0,
    ) -> Optional[Any]:
        """
        解码健康数据记录中的 rawData 字段
        
        Args:
            record: 健康数据记录字典，应包含 rawData 和 formatType 字段
                示例:
                {
                    "id": 1,
                    "userId": 1,
                    "dataType": "REPORT",
                    "formatType": "IMAGE",
                    "rawData": "iVBORw0KGgoAAAANSUhEUgAA...",
                    ...
                }
        
        Returns:
            解码后的数据，类型取决于 formatType:
            - IMAGE / PDF / CSV: bytes
            - JSON: dict
            - TEXT: str
            - 其他格式: bytes
            - rawData 为空或不存在: None
            
        Raises:
            ValueError: 当 Base64 解码失败时
            json.JSONDecodeError: 当 JSON 格式无效时（仅 JSON 格式）
            UnicodeDecodeError: 当文本解码失败时（仅 TEXT 格式）
        """
        # 检查 rawData 是否存在或为空
        raw_data = record.get("rawData")
        if not raw_data:
            logger.debug("rawData 为空或不存在，返回 None")
            return None
        
        # 获取 formatType，默认为空字符串
        format_type = record.get("formatType", "").upper()
        logger.debug(f"开始解码 rawData，formatType: {format_type}")
        
        try:
            # Base64 解码
            decoded_bytes = base64.b64decode(raw_data)
            logger.debug(f"Base64 解码成功，数据大小: {len(decoded_bytes)} 字节")
            
        except Exception as e:
            error_msg = f"Base64 解码失败: {str(e)}, formatType: {format_type}"
            logger.error(error_msg)
            raise ValueError(error_msg) from e
        
        # 根据 formatType 处理数据
        if format_type in self.BINARY_FORMATS:
            # IMAGE / PDF / CSV：直接返回 bytes
            logger.debug(f"格式类型 {format_type}，返回 bytes")
            if pipeline_user_id is not None:
                log_after_decode(
                    pipeline_user_id,
                    pipeline_idx,
                    record.get("id"),
                    str(record.get("dataType", "")),
                    format_type,
                    decoded_bytes,
                )
            return decoded_bytes
            
        elif format_type == self.JSON_FORMAT:
            # JSON：Base64 解码后再 json.loads
            try:
                # 先尝试将 bytes 解码为 UTF-8 字符串
                json_str = decoded_bytes.decode("utf-8")
                json_data = json.loads(json_str)
                logger.debug(f"JSON 解析成功，数据类型: {type(json_data)}")
                if pipeline_user_id is not None:
                    log_after_decode(
                        pipeline_user_id,
                        pipeline_idx,
                        record.get("id"),
                        str(record.get("dataType", "")),
                        format_type,
                        json_data,
                    )
                return json_data
            except UnicodeDecodeError as e:
                error_msg = f"JSON 数据 UTF-8 解码失败: {str(e)}"
                logger.error(error_msg)
                raise ValueError(error_msg) from e
            except json.JSONDecodeError:
                # 重新抛出 JSONDecodeError，保留原始异常信息
                logger.error("JSON 解析失败")
                raise 
                
        elif format_type == self.TEXT_FORMAT:
            # TEXT：Base64 解码后返回 str
            try:
                text = decoded_bytes.decode("utf-8")
                logger.debug(f"文本解码成功，长度: {len(text)} 字符")
                if pipeline_user_id is not None:
                    log_after_decode(
                        pipeline_user_id,
                        pipeline_idx,
                        record.get("id"),
                        str(record.get("dataType", "")),
                        format_type,
                        text,
                    )
                return text
            except UnicodeDecodeError as e:
                error_msg = f"文本 UTF-8 解码失败: {str(e)}"
                logger.error(error_msg)
                raise ValueError(error_msg) from e
        else:
            # 未识别的 formatType：直接返回解码后的 bytes
            logger.warning(f"未识别的 formatType: {format_type}，返回 bytes")
            if pipeline_user_id is not None:
                log_after_decode(
                    pipeline_user_id,
                    pipeline_idx,
                    record.get("id"),
                    str(record.get("dataType", "")),
                    format_type or "UNKNOWN",
                    decoded_bytes,
                )
            return decoded_bytes
