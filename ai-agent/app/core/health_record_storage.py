"""
健康档案存储模块
使用内存字典存储健康档案数据
"""
import logging
from typing import Optional, Dict, List, Any, Literal
from datetime import datetime

# 配置日志
logger = logging.getLogger(__name__)

# 健康档案状态枚举
ArchiveStatus = Literal["READY", "DIRTY", "UPDATING"]
DEFAULT_ARCHIVE_STATUS: ArchiveStatus = "READY"


class HealthRecordStorage:
    """健康档案内存存储类"""
    
    def __init__(self):
        """
        初始化健康档案存储
        
        存储结构：
        {
            user_id: {
                record_id: {
                    "content": str,
                    "created_at": str,
                    "updated_at": str,
                    "archive_status": str  # READY | DIRTY | UPDATING，默认 READY
                }
            }
        }
        
        数据版本信息存储结构：
        {
            user_id: {
                "needsRefresh": bool,  # 是否需要刷新标记
                "dataVersion": {
                    "dataIds": List[int],  # 数据ID列表
                    "latestUpdateTime": str,  # 最新更新时间
                    "count": int  # 数据条数
                }
            }
        }
        """
        # 使用嵌套字典存储：{user_id: {record_id: record_data}}
        # record_data 类型：Dict[str, Any]，包含 content, created_at, updated_at, archive_status
        self._storage: Dict[int, Dict[str, Dict[str, Any]]] = {}
        # 数据版本信息存储：{user_id: {needsRefresh: bool, dataVersion: {...}}}
        self._version_storage: Dict[int, Dict[str, Any]] = {}
        logger.info("健康档案存储初始化完成")
    
    def save_record(
        self, 
        user_id: int, 
        record_id: str, 
        content: str
    ) -> None:
        """
        保存健康档案记录
        
        Args:
            user_id: 用户ID
            record_id: 记录ID（唯一标识）
            content: 健康档案内容（结构化文本）
        
        Raises:
            ValueError: 当参数无效时
        """
        if not isinstance(user_id, int) or user_id <= 0:
            raise ValueError(f"无效的用户ID: {user_id}，必须为正整数")
        
        if not record_id or not isinstance(record_id, str):
            raise ValueError(f"无效的记录ID: {record_id}，必须为非空字符串")
        
        if not isinstance(content, str):
            raise ValueError(f"内容必须是字符串类型，当前类型: {type(content)}")
        
        # 获取当前时间戳
        now = datetime.now().isoformat()
        
        # 初始化用户存储空间（如果不存在）
        if user_id not in self._storage:
            self._storage[user_id] = {}
            logger.debug(f"为用户 {user_id} 创建存储空间")
        
        # 检查记录是否已存在
        is_update = record_id in self._storage[user_id]
        
        # 保存或更新记录
        # 如果是更新，保留原有的 archive_status；如果是新建，使用默认值 READY
        existing_status = None
        if is_update:
            existing_status = self._storage[user_id][record_id].get("archive_status", DEFAULT_ARCHIVE_STATUS)
        
        self._storage[user_id][record_id] = {
            "content": content,
            "created_at": self._storage[user_id][record_id].get("created_at", now) if is_update else now,
            "updated_at": now,
            "archive_status": existing_status if is_update else DEFAULT_ARCHIVE_STATUS
        }
        
        action = "更新" if is_update else "创建"
        logger.info(f"{action}健康档案：用户 {user_id}，记录ID {record_id}，内容长度 {len(content)} 字符")
    
    def get_record(
        self, 
        user_id: int, 
        record_id: str
    ) -> Optional[str]:
        """
        获取健康档案记录
        
        Args:
            user_id: 用户ID
            record_id: 记录ID
        
        Returns:
            健康档案内容，如果记录不存在则返回 None
        
        Raises:
            ValueError: 当参数无效时
        """
        if not isinstance(user_id, int) or user_id <= 0:
            raise ValueError(f"无效的用户ID: {user_id}，必须为正整数")
        
        if not record_id or not isinstance(record_id, str):
            raise ValueError(f"无效的记录ID: {record_id}，必须为非空字符串")
        
        # 检查用户是否存在
        if user_id not in self._storage:
            logger.debug(f"用户 {user_id} 不存在")
            return None
        
        # 检查记录是否存在
        if record_id not in self._storage[user_id]:
            logger.debug(f"用户 {user_id} 的记录 {record_id} 不存在")
            return None
        
        # 返回记录内容
        record = self._storage[user_id][record_id]
        logger.debug(f"获取健康档案：用户 {user_id}，记录ID {record_id}")
        return record["content"]
    
    def get_archive_status(
        self,
        user_id: int,
        record_id: str
    ) -> Optional[ArchiveStatus]:
        """
        获取健康档案的状态
        
        Args:
            user_id: 用户ID
            record_id: 记录ID
        
        Returns:
            健康档案状态（READY | DIRTY | UPDATING），如果记录不存在则返回 None
        
        Raises:
            ValueError: 当参数无效时
        """
        if not isinstance(user_id, int) or user_id <= 0:
            raise ValueError(f"无效的用户ID: {user_id}，必须为正整数")
        
        if not record_id or not isinstance(record_id, str):
            raise ValueError(f"无效的记录ID: {record_id}，必须为非空字符串")
        
        # 检查用户是否存在
        if user_id not in self._storage:
            logger.debug(f"用户 {user_id} 不存在")
            return None
        
        # 检查记录是否存在
        if record_id not in self._storage[user_id]:
            logger.debug(f"用户 {user_id} 的记录 {record_id} 不存在")
            return None
        
        # 返回状态（向后兼容：如果旧数据没有状态，默认返回 READY）
        record = self._storage[user_id][record_id]
        status = record.get("archive_status", DEFAULT_ARCHIVE_STATUS)
        logger.debug(f"获取健康档案状态：用户 {user_id}，记录ID {record_id}，状态={status}")
        return status
    
    def set_archive_status(
        self,
        user_id: int,
        record_id: str,
        status: ArchiveStatus
    ) -> bool:
        """
        设置健康档案的状态
        
        Args:
            user_id: 用户ID
            record_id: 记录ID
            status: 状态值（READY | DIRTY | UPDATING）
        
        Returns:
            设置成功返回 True，记录不存在返回 False
        
        Raises:
            ValueError: 当参数无效时
        """
        if not isinstance(user_id, int) or user_id <= 0:
            raise ValueError(f"无效的用户ID: {user_id}，必须为正整数")
        
        if not record_id or not isinstance(record_id, str):
            raise ValueError(f"无效的记录ID: {record_id}，必须为非空字符串")
        
        if status not in ("READY", "DIRTY", "UPDATING"):
            raise ValueError(f"无效的状态值: {status}，必须是 READY、DIRTY 或 UPDATING")
        
        # 检查用户是否存在
        if user_id not in self._storage:
            logger.debug(f"用户 {user_id} 不存在")
            return False
        
        # 检查记录是否存在
        if record_id not in self._storage[user_id]:
            logger.debug(f"用户 {user_id} 的记录 {record_id} 不存在")
            return False
        
        # 设置状态
        self._storage[user_id][record_id]["archive_status"] = status
        logger.info(f"设置健康档案状态：用户 {user_id}，记录ID {record_id}，状态={status}")
        return True
    
    def list_records(self, user_id: int) -> List[Dict[str, str]]:
        """
        列出用户的所有健康档案记录
        
        Args:
            user_id: 用户ID
        
        Returns:
            健康档案记录列表，每个记录包含：
            - record_id: 记录ID
            - content: 健康档案内容（截断预览）
            - created_at: 创建时间
            - updated_at: 更新时间
        
        Raises:
            ValueError: 当参数无效时
        """
        if not isinstance(user_id, int) or user_id <= 0:
            raise ValueError(f"无效的用户ID: {user_id}，必须为正整数")
        
        # 检查用户是否存在
        if user_id not in self._storage:
            logger.debug(f"用户 {user_id} 不存在，返回空列表")
            return []
        
        # 构建记录列表
        records = []
        for record_id, record_data in self._storage[user_id].items():
            records.append({
                "record_id": record_id,
                "content_preview": record_data["content"][:100] + "..." if len(record_data["content"]) > 100 else record_data["content"],
                "content_length": len(record_data["content"]),
                "created_at": record_data["created_at"],
                "updated_at": record_data["updated_at"],
                "archive_status": record_data.get("archive_status", DEFAULT_ARCHIVE_STATUS)  # 向后兼容：如果旧数据没有状态，默认 READY
            })
        
        logger.info(f"列出用户 {user_id} 的健康档案，共 {len(records)} 条记录")
        return records
    
    def delete_record(self, user_id: int, record_id: str) -> bool:
        """
        删除健康档案记录（可选功能）
        
        Args:
            user_id: 用户ID
            record_id: 记录ID
        
        Returns:
            删除成功返回 True，记录不存在返回 False
        
        Raises:
            ValueError: 当参数无效时
        """
        if not isinstance(user_id, int) or user_id <= 0:
            raise ValueError(f"无效的用户ID: {user_id}，必须为正整数")
        
        if not record_id or not isinstance(record_id, str):
            raise ValueError(f"无效的记录ID: {record_id}，必须为非空字符串")
        
        # 检查用户是否存在
        if user_id not in self._storage:
            logger.debug(f"用户 {user_id} 不存在")
            return False
        
        # 检查记录是否存在
        if record_id not in self._storage[user_id]:
            logger.debug(f"用户 {user_id} 的记录 {record_id} 不存在")
            return False
        
        # 删除记录
        del self._storage[user_id][record_id]
        logger.info(f"删除健康档案：用户 {user_id}，记录ID {record_id}")
        
        # 如果用户没有其他记录，清理用户存储空间
        if not self._storage[user_id]:
            del self._storage[user_id]
            logger.debug(f"用户 {user_id} 没有更多记录，清理存储空间")
        
        return True
    
    def get_stats(self) -> Dict[str, int]:
        """
        获取存储统计信息（可选功能，用于调试）
        
        Returns:
            统计信息字典，包含：
            - total_users: 总用户数
            - total_records: 总记录数
        """
        total_users = len(self._storage)
        total_records = sum(len(records) for records in self._storage.values())
        
        stats = {
            "total_users": total_users,
            "total_records": total_records
        }
        
        logger.debug(f"存储统计：{stats}")
        return stats
    
    def save_data_version(
        self,
        user_id: int,
        data_ids: List[int],
        latest_update_time: str
    ) -> None:
        """
        保存数据版本信息
        
        Args:
            user_id: 用户ID
            data_ids: 数据ID列表
            latest_update_time: 最新更新时间（ISO格式字符串）
        
        Raises:
            ValueError: 当参数无效时
        """
        if not isinstance(user_id, int) or user_id <= 0:
            raise ValueError(f"无效的用户ID: {user_id}，必须为正整数")
        
        if not isinstance(data_ids, list):
            raise ValueError(f"data_ids 必须是列表类型，当前类型: {type(data_ids)}")
        
        if not isinstance(latest_update_time, str):
            raise ValueError(f"latest_update_time 必须是字符串类型，当前类型: {type(latest_update_time)}")
        
        # 初始化用户版本存储空间（如果不存在）
        if user_id not in self._version_storage:
            self._version_storage[user_id] = {}
        
        # 保存版本信息
        self._version_storage[user_id]["dataVersion"] = {
            "dataIds": sorted(data_ids),  # 排序以便比较
            "latestUpdateTime": latest_update_time,
            "count": len(data_ids)
        }
        
        # 清除刷新标记（因为已经更新了版本信息）
        self._version_storage[user_id]["needsRefresh"] = False
        
        logger.info(
            f"保存用户 {user_id} 的数据版本信息："
            f"数据ID数量={len(data_ids)}, 最新更新时间={latest_update_time}"
        )
    
    def get_data_version(self, user_id: int) -> Optional[Dict[str, Any]]:
        """
        获取数据版本信息
        
        Args:
            user_id: 用户ID
        
        Returns:
            数据版本信息字典，如果不存在则返回 None
            格式：{"dataIds": [1, 2, 3], "latestUpdateTime": "...", "count": 3}
        
        Raises:
            ValueError: 当参数无效时
        """
        if not isinstance(user_id, int) or user_id <= 0:
            raise ValueError(f"无效的用户ID: {user_id}，必须为正整数")
        
        if user_id not in self._version_storage:
            logger.debug(f"用户 {user_id} 没有版本信息")
            return None
        
        version_info = self._version_storage[user_id].get("dataVersion")
        if version_info is None:
            logger.debug(f"用户 {user_id} 的版本信息为空")
            return None
        
        logger.debug(f"获取用户 {user_id} 的数据版本信息")
        return version_info.copy()
    
    def check_data_changed(
        self,
        user_id: int,
        current_data_ids: List[int],
        current_latest_time: str
    ) -> bool:
        """
        检查数据是否变化
        
        Args:
            user_id: 用户ID
            current_data_ids: 当前数据ID列表
            current_latest_time: 当前最新更新时间
        
        Returns:
            True 表示数据有变化，False 表示无变化
        
        Raises:
            ValueError: 当参数无效时
        """
        if not isinstance(user_id, int) or user_id <= 0:
            raise ValueError(f"无效的用户ID: {user_id}，必须为正整数")
        
        # 如果没有版本信息，认为数据有变化（需要首次加载）
        stored_version = self.get_data_version(user_id)
        if stored_version is None:
            logger.debug(f"用户 {user_id} 没有存储的版本信息，认为数据有变化")
            return True
        
        # 比较数据ID列表
        stored_ids = set(stored_version.get("dataIds", []))
        current_ids = set(current_data_ids)
        
        if stored_ids != current_ids:
            logger.info(
                f"用户 {user_id} 的数据ID列表发生变化："
                f"存储={sorted(stored_ids)}, 当前={sorted(current_ids)}"
            )
            return True
        
        # 比较最新更新时间
        stored_time = stored_version.get("latestUpdateTime", "")
        if stored_time != current_latest_time:
            logger.info(
                f"用户 {user_id} 的数据更新时间发生变化："
                f"存储={stored_time}, 当前={current_latest_time}"
            )
            return True
        
        logger.debug(f"用户 {user_id} 的数据未发生变化")
        return False
    
    def mark_needs_refresh(self, user_id: int) -> None:
        """
        标记用户需要刷新健康档案
        
        Args:
            user_id: 用户ID
        
        Raises:
            ValueError: 当参数无效时
        """
        if not isinstance(user_id, int) or user_id <= 0:
            raise ValueError(f"无效的用户ID: {user_id}，必须为正整数")
        
        # 初始化用户版本存储空间（如果不存在）
        if user_id not in self._version_storage:
            self._version_storage[user_id] = {}
        
        # 设置刷新标记
        self._version_storage[user_id]["needsRefresh"] = True
        logger.info(f"标记用户 {user_id} 需要刷新健康档案")
    
    def needs_refresh(self, user_id: int) -> bool:
        """
        检查用户是否需要刷新健康档案
        
        Args:
            user_id: 用户ID
        
        Returns:
            True 表示需要刷新，False 表示不需要
        
        Raises:
            ValueError: 当参数无效时
        """
        if not isinstance(user_id, int) or user_id <= 0:
            raise ValueError(f"无效的用户ID: {user_id}，必须为正整数")
        
        if user_id not in self._version_storage:
            logger.debug(f"用户 {user_id} 没有版本存储信息，不需要刷新")
            return False
        
        needs_refresh = self._version_storage[user_id].get("needsRefresh", False)
        logger.debug(f"用户 {user_id} 的刷新标记: {needs_refresh}")
        return needs_refresh
    
    def clear_refresh_flag(self, user_id: int) -> None:
        """
        清除用户的刷新标记
        
        Args:
            user_id: 用户ID
        
        Raises:
            ValueError: 当参数无效时
        """
        if not isinstance(user_id, int) or user_id <= 0:
            raise ValueError(f"无效的用户ID: {user_id}，必须为正整数")
        
        if user_id in self._version_storage:
            self._version_storage[user_id]["needsRefresh"] = False
            logger.debug(f"清除用户 {user_id} 的刷新标记")