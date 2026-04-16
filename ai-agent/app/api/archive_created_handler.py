"""
健康档案创建 Webhook 处理逻辑
当后端生成新档案后调用，智能体拉取数据、生成分析建议并提交
"""
import json
import logging
from typing import Optional

from app.core.config import settings
from app.services.health_analysis_service import generate_health_analysis
from app.services.health_recommendation_service import generate_health_recommendation
from app.db.health_archive_process_client import (
    fetch_user_health_data,
    submit_health_analysis,
    submit_health_recommendation,
    convert_recommendation_result_to_api_format,
    update_health_archive_by_ai
)

logger = logging.getLogger(__name__)


async def execute_archive_created_task(
    user_id: int,
    archive_id: int,
    orchestrator
):
    """
    执行健康档案创建后的任务：拉取数据、生成分析建议、提交到后端
    """
    try:
        logger.info(f"[ARCHIVE_CREATED] 开始处理：userId={user_id}, archiveId={archive_id}")

        # 1. 从后端拉取用户完整数据
        data = fetch_user_health_data(user_id)
        if not data:
            logger.warning(f"[ARCHIVE_CREATED] 用户 {user_id} 无健康数据，跳过")
            return

        health_archives = data.get("healthArchives", [])
        archive_for_storage = None
        for item in health_archives:
            info = item.get("healthArchive", {}) if isinstance(item, dict) else {}
            aid = info.get("archiveId") if isinstance(info, dict) else item.get("archiveId")
            if aid == archive_id:
                archive_for_storage = item
                break
        if not archive_for_storage and health_archives:
            archive_for_storage = health_archives[0]

        # 2. 保存档案到存储，供 generate_health_analysis 使用
        if archive_for_storage and orchestrator and hasattr(orchestrator, "health_record_storage"):
            storage = orchestrator.health_record_storage
            archive_json_str = json.dumps(archive_for_storage, ensure_ascii=False)
            storage.save_record(user_id=user_id, record_id="latest", content=archive_json_str)
            logger.info(f"[ARCHIVE_CREATED] 档案已保存到存储")

        # 3. 生成健康分析
        llm = None
        if hasattr(orchestrator, "llm"):
            llm = orchestrator.llm
        if not llm:
            try:
                from app.api.routes import get_llm
                llm = get_llm()
            except Exception:
                pass
        if not llm:
            logger.error("[ARCHIVE_CREATED] 无法获取 LLM，跳过分析")
            return

        analysis_result = generate_health_analysis(user_id, llm)
        source_raw_id = 0
        raw_list = data.get("rawHealthDataList", [])
        if raw_list and isinstance(raw_list[0], dict):
            source_raw_id = raw_list[0].get("id", 0) or 0

        # 4. 提交健康分析
        analysis_payload = {
            "archiveId": archive_id,
            "sourceRawId": source_raw_id,
            "modelName": settings.llm_model,
            "modelVersion": "1.0",
            "analysisResult": analysis_result
        }
        submit_health_analysis(user_id, analysis_payload)
        logger.info(f"[ARCHIVE_CREATED] 健康分析已提交")

        # 4.1 回填健康档案主表/从表，确保用户查看档案时看到完整信息
        if archive_for_storage:
            try:
                update_health_archive_by_ai(
                    user_id=user_id,
                    archive_id=archive_id,
                    archive_detail=archive_for_storage,
                    analysis_result=analysis_result
                )
                logger.info(f"[ARCHIVE_CREATED] 健康档案主从表回填完成")
            except Exception as e:
                # 不中断主流程，保证健康总结/建议仍可写入
                logger.warning(f"[ARCHIVE_CREATED] 健康档案主从表回填失败，继续建议提交流程: {e}")

        # 5. 生成并提交健康建议
        recommendation_result = generate_health_recommendation(user_id, analysis_result=analysis_result)
        recommendations = convert_recommendation_result_to_api_format(recommendation_result)
        rec_payload = {
            **analysis_payload,
            "recommendations": recommendations
        }
        submit_health_recommendation(user_id, rec_payload)
        logger.info(f"[ARCHIVE_CREATED] 健康建议已提交")

        logger.info(f"[ARCHIVE_CREATED] 处理完成：userId={user_id}, archiveId={archive_id}")

    except Exception as e:
        logger.error(f"[ARCHIVE_CREATED] 处理失败：userId={user_id}, archiveId={archive_id}, error={e}", exc_info=True)
