"""
健康数据 HTTP 客户端
用于从外部 API 读取用户健康数据（重建流程用）。勿使用 3306 端口（为 MySQL）。
"""
import os
import logging
from typing import List, Dict, Any

try:
    from app.core.config import settings
    DEFAULT_BASE_URL = getattr(settings, "health_api_base_url", "http://localhost:8080")
except ImportError:
    DEFAULT_BASE_URL = "http://localhost:8080"

try:
    import requests
    HAS_REQUESTS = True
except ImportError:
    HAS_REQUESTS = False
    logging.warning("requests 库未安装，请安装: pip install requests")

# 配置日志
logger = logging.getLogger(__name__)


class HealthDataClient:
    """健康数据 HTTP 客户端"""
    
    def __init__(self, base_url: str = None, timeout: int = 5):
        """
        初始化健康数据客户端
        
        Args:
            base_url: API 基础 URL（默认使用 DEFAULT_BASE_URL）
            timeout: 请求超时时间（秒），默认 5 秒
            
        Raises:
            ValueError: 当 base_url 未设置时
            ImportError: 当 requests 库未安装时
        """
        if not HAS_REQUESTS:
            raise ImportError("请安装 requests 库: pip install requests")
        
        # 从环境变量或参数获取 base_url
        self.base_url = (base_url or DEFAULT_BASE_URL).rstrip("/")
        self.timeout = timeout
        
        logger.info(f"健康数据客户端初始化完成，BASE_URL: {self.base_url}, 超时时间: {self.timeout}秒")
    
    def fetch_user_health_data(self, user_id: int) -> List[Dict[str, Any]]:
        """
        获取指定用户的健康数据
        
        Args:
            user_id: 用户ID
            
        Returns:
            健康数据列表，格式为 response.json()["data"]
            
        Raises:
            RuntimeError: 当 HTTP 状态码不是 200 或响应 JSON 中 code != 200 时
            requests.RequestException: 当请求发生网络错误时
            ValueError: 当 user_id 无效时
        """
        if not isinstance(user_id, int) or user_id <= 0:
            raise ValueError(f"无效的用户ID: {user_id}，必须为正整数")
        
        # 构建请求 URL
        url = f"{self.base_url}/raw-health-data/user/{user_id}"
        logger.info(f"正在请求用户 {user_id} 的健康数据: {url}")
        
        try:
            # 发送 GET 请求
            response = requests.get(url, timeout=self.timeout)
            
            # 检查 HTTP 状态码
            if response.status_code != 200:
                error_msg = (
                    f"HTTP 请求失败，状态码: {response.status_code}, "
                    f"URL: {url}, 响应: {response.text[:200]}"
                )
                logger.error(error_msg)
                raise RuntimeError(error_msg)
            
            # 解析 JSON 响应
            try:
                json_data = response.json()
            except ValueError as e:
                error_msg = f"响应不是有效的 JSON 格式: {str(e)}, URL: {url}"
                logger.error(error_msg)
                raise RuntimeError(error_msg) from e
            
            # 检查响应中的 code 字段
            if json_data.get("code") != 200:
                code = json_data.get("code", "unknown")
                message = json_data.get("message", "未知错误")
                error_msg = f"API 返回错误，code: {code}, message: {message}, URL: {url}"
                logger.error(error_msg)
                raise RuntimeError(error_msg)
            
            # 提取并返回 data 字段
            data = json_data.get("data", [])
            if not isinstance(data, list):
                error_msg = f"响应中的 data 字段不是列表类型: {type(data)}, URL: {url}"
                logger.error(error_msg)
                raise RuntimeError(error_msg)
            
            logger.info(f"成功获取用户 {user_id} 的健康数据，共 {len(data)} 条记录")
            return data
            
        except requests.Timeout as e:
            error_msg = f"请求超时（{self.timeout}秒）: {url}"
            logger.error(error_msg)
            raise RuntimeError(error_msg) from e
            
        except requests.RequestException as e:
            error_msg = f"网络请求异常: {str(e)}, URL: {url}"
            logger.error(error_msg)
            raise RuntimeError(error_msg) from e
            
        except RuntimeError:
            # 重新抛出 RuntimeError（已经记录过日志）
            raise
            
        except Exception as e:
            # 捕获其他未预期的异常
            error_msg = f"获取健康数据时发生未知错误: {str(e)}, URL: {url}"
            logger.error(error_msg, exc_info=True)
            raise RuntimeError(error_msg) from e
    
    def fetch_user_health_data_metadata(self, user_id: int) -> Dict[str, Any]:
        """
        获取指定用户的健康数据元数据（轻量级查询，不包含完整数据）
        
        只获取数据ID、更新时间等元数据，不获取完整的rawData内容。
        用于快速检查数据是否变化。
        
        Args:
            user_id: 用户ID
            
        Returns:
            元数据字典，格式：
            {
                "dataIds": [1, 2, 3],  # 数据ID列表
                "latestUpdateTime": "2025-01-15T10:30:00",  # 最新更新时间
                "count": 3  # 数据条数
            }
            
        Raises:
            RuntimeError: 当 HTTP 状态码不是 200 或响应 JSON 中 code != 200 时
            requests.RequestException: 当请求发生网络错误时
            ValueError: 当 user_id 无效时
        """
        if not isinstance(user_id, int) or user_id <= 0:
            raise ValueError(f"无效的用户ID: {user_id}，必须为正整数")
        
        # 构建请求 URL（使用相同的接口，但只提取元数据）
        url = f"{self.base_url}/raw-health-data/user/{user_id}"
        logger.info(f"正在请求用户 {user_id} 的健康数据元数据: {url}")
        
        try:
            # 发送 GET 请求
            response = requests.get(url, timeout=self.timeout)
            
            # 检查 HTTP 状态码
            if response.status_code != 200:
                error_msg = (
                    f"HTTP 请求失败，状态码: {response.status_code}, "
                    f"URL: {url}, 响应: {response.text[:200]}"
                )
                logger.error(error_msg)
                raise RuntimeError(error_msg)
            
            # 解析 JSON 响应
            try:
                json_data = response.json()
            except ValueError as e:
                error_msg = f"响应不是有效的 JSON 格式: {str(e)}, URL: {url}"
                logger.error(error_msg)
                raise RuntimeError(error_msg) from e
            
            # 检查响应中的 code 字段
            if json_data.get("code") != 200:
                code = json_data.get("code", "unknown")
                message = json_data.get("message", "未知错误")
                error_msg = f"API 返回错误，code: {code}, message: {message}, URL: {url}"
                logger.error(error_msg)
                raise RuntimeError(error_msg)
            
            # 提取 data 字段
            data = json_data.get("data", [])
            if not isinstance(data, list):
                error_msg = f"响应中的 data 字段不是列表类型: {type(data)}, URL: {url}"
                logger.error(error_msg)
                raise RuntimeError(error_msg)
            
            # 提取元数据（不包含完整的rawData）
            data_ids = []
            latest_update_time = None
            
            for record in data:
                # 提取数据ID
                record_id = record.get("id")
                if record_id is not None:
                    data_ids.append(record_id)
                
                # 提取更新时间（假设字段名为 updateTime 或 updatedAt）
                update_time = record.get("updateTime") or record.get("updatedAt") or record.get("update_time")
                if update_time:
                    if latest_update_time is None or update_time > latest_update_time:
                        latest_update_time = update_time
            
            # 如果没有找到更新时间，使用当前时间作为默认值
            if latest_update_time is None:
                from datetime import datetime
                latest_update_time = datetime.now().isoformat()
            
            metadata = {
                "dataIds": sorted(data_ids),
                "latestUpdateTime": latest_update_time,
                "count": len(data_ids)
            }
            
            logger.info(
                f"成功获取用户 {user_id} 的健康数据元数据："
                f"数据ID数量={len(data_ids)}, 最新更新时间={latest_update_time}"
            )
            return metadata
            
        except requests.Timeout as e:
            error_msg = f"请求超时（{self.timeout}秒）: {url}"
            logger.error(error_msg)
            raise RuntimeError(error_msg) from e
            
        except requests.RequestException as e:
            error_msg = f"网络请求异常: {str(e)}, URL: {url}"
            logger.error(error_msg)
            raise RuntimeError(error_msg) from e
            
        except RuntimeError:
            # 重新抛出 RuntimeError（已经记录过日志）
            raise
            
        except Exception as e:
            # 捕获其他未预期的异常
            error_msg = f"获取健康数据元数据时发生未知错误: {str(e)}, URL: {url}"
            logger.error(error_msg, exc_info=True)
            raise RuntimeError(error_msg) from e