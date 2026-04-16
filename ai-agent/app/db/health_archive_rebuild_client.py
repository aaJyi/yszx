"""
健康档案重建客户端
用于触发外部系统的健康档案更新流程
"""
import logging
from typing import Dict, Any

try:
    import requests
    HAS_REQUESTS = True
except ImportError:
    HAS_REQUESTS = False
    logging.warning("requests 库未安装，请安装: pip install requests")

# 配置日志
logger = logging.getLogger(__name__)

try:
    from app.core.config import settings
    DEFAULT_BASE_URL = getattr(settings, "health_api_base_url", "http://localhost:8080")
except ImportError:
    DEFAULT_BASE_URL = "http://localhost:8080"


class HealthArchiveRebuildNetworkError(Exception):
    """健康档案重建网络错误异常"""
    pass


class HealthArchiveRebuildBusinessError(Exception):
    """健康档案重建业务失败异常"""
    pass


class HealthArchiveRebuildClient:
    """健康档案重建客户端"""
    
    def __init__(self, base_url: str = None, timeout: int = 5):
        """
        初始化健康档案重建客户端
        
        Args:
            base_url: API 基础 URL（默认使用 DEFAULT_BASE_URL）
            timeout: 请求超时时间（秒），默认 5 秒
            
        Raises:
            ImportError: 当 requests 库未安装时
        """
        if not HAS_REQUESTS:
            raise ImportError("请安装 requests 库: pip install requests")
        
        # 从环境变量或参数获取 base_url
        self.base_url = (base_url or DEFAULT_BASE_URL).rstrip("/")
        self.timeout = timeout
        
        logger.info(f"健康档案重建客户端初始化完成，BASE_URL: {self.base_url}, 超时时间: {self.timeout}秒")
    
    def trigger_rebuild(self, user_id: int, event: str, update_time: str = None) -> Dict[str, Any]:
        """
        触发外部系统的健康档案更新流程
        
        调用外部接口 POST /health-archive-process/rebuild 触发健康档案重建。
        不包含任何健康档案解析、生成、缓存逻辑，仅负责触发外部流程。
        
        Args:
            user_id: 用户ID
            event: 事件类型（如 "data_updated", "data_created", "data_deleted"）
            update_time: 更新时间（可选），ISO 8601 格式字符串
        
        Returns:
            API 响应的 JSON 数据（response.json()）
        
        Raises:
            ValueError: 当 user_id 或 event 无效时
            ImportError: 当 requests 库未安装时
            HealthArchiveRebuildNetworkError: 当发生网络错误时（连接失败、超时等）
            HealthArchiveRebuildBusinessError: 当业务逻辑失败时（HTTP状态码异常、业务错误等）
        """
        if not isinstance(user_id, int) or user_id <= 0:
            raise ValueError(f"无效的用户ID: {user_id}，必须为正整数")
        
        if not isinstance(event, str) or not event.strip():
            raise ValueError(f"无效的事件类型: {event}，必须为非空字符串")
        
        # 构建请求 URL
        url = f"{self.base_url}/health-archive-process/rebuild"
        
        # 构建请求体
        payload = {
            "userId": user_id,
            "event": event.strip()
        }
        
        # 如果提供了 updateTime，添加到请求体中
        if update_time is not None:
            if not isinstance(update_time, str) or not update_time.strip():
                raise ValueError(f"无效的更新时间: {update_time}，必须为非空字符串")
            payload["updateTime"] = update_time.strip()
        
        # 设置请求头
        headers = {
            "Content-Type": "application/json"
        }
        
        logger.info(f"触发用户 {user_id} 的健康档案重建，事件: {event}, URL: {url}")
        
        try:
            # 发送 POST 请求
            response = requests.post(
                url,
                json=payload,
                headers=headers,
                timeout=self.timeout
            )
        except requests.exceptions.Timeout as e:
            error_msg = f"健康档案重建请求超时（{self.timeout}秒）: {url}"
            logger.error(error_msg)
            raise HealthArchiveRebuildNetworkError(error_msg) from e
        except requests.exceptions.ConnectionError as e:
            error_msg = f"健康档案重建请求连接失败: {url}, 错误: {str(e)}"
            logger.error(error_msg)
            raise HealthArchiveRebuildNetworkError(error_msg) from e
        except requests.exceptions.RequestException as e:
            error_msg = f"健康档案重建请求网络异常: {url}, 错误: {str(e)}"
            logger.error(error_msg)
            raise HealthArchiveRebuildNetworkError(error_msg) from e
        
        # 检查 HTTP 状态码（区分网络错误和业务失败）
        if response.status_code != 200:
            error_msg = (
                f"健康档案重建 API 返回异常状态码: {response.status_code}, "
                f"URL: {url}, 响应: {response.text[:200]}"
            )
            logger.error(error_msg)
            raise HealthArchiveRebuildBusinessError(error_msg)
        
        # 解析 JSON 响应
        try:
            json_data = response.json()
        except ValueError as e:
            error_msg = f"健康档案重建 API 返回非 JSON 格式: {str(e)}, URL: {url}, 响应: {response.text[:200]}"
            logger.error(error_msg)
            raise HealthArchiveRebuildBusinessError(error_msg) from e
        
        # 检查响应中的业务状态码（如果存在 code 字段）
        response_code = json_data.get("code")
        if response_code is not None and response_code != 200:
            code = response_code
            message = json_data.get("message", "未知错误")
            error_msg = f"健康档案重建业务失败，code: {code}, message: {message}, URL: {url}"
            logger.error(error_msg)
            raise HealthArchiveRebuildBusinessError(error_msg)
        
        logger.info(f"成功触发用户 {user_id} 的健康档案重建，事件: {event}")
        return json_data
