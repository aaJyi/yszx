"""
API 路由
"""
import uuid
import logging
import asyncio
from datetime import datetime
from fastapi import APIRouter, HTTPException, Depends
from pydantic import BaseModel
from typing import List, Optional, Dict, Any

from app.core.config import settings
from app.core.llm_client import LlmClient
from app.core.medical_prompts import build_organize_prompt, build_analyze_prompt
from app.core.multimodal_builder import build_medical_messages
from app.core.health_record_storage import HealthRecordStorage
from app.services.chat_orchestrator import ChatOrchestrator
from app.db.health_archive_rebuild_client import (
    HealthArchiveRebuildClient,
    HealthArchiveRebuildNetworkError,
    HealthArchiveRebuildBusinessError
)
from app.services.health_analysis_service import generate_health_analysis
from app.services.health_recommendation_service import generate_health_recommendation
from app.db.health_archive_process_client import (
    submit_health_analysis,
    submit_health_recommendation,
    get_latest_archive_id,
    convert_recommendation_result_to_api_format
)

router = APIRouter()

# 配置日志
logger = logging.getLogger(__name__)

# 这些变量将在 main.py 中通过依赖注入覆盖
_llm = None
_storage = None
_orchestrator = None


def get_llm() -> LlmClient:
    """获取 LLM 实例（依赖注入）"""
    return _llm


def get_storage() -> HealthRecordStorage:
    """获取 HealthRecordStorage 实例（依赖注入）"""
    return _storage


def get_orchestrator() -> ChatOrchestrator:
    """获取 ChatOrchestrator 实例（依赖注入）"""
    return _orchestrator


class SourceInfo(BaseModel):
    """来源信息模型"""
    title: str
    source: str
    chunk_id: int


class OrganizeRequest(BaseModel):
    """健康档案整理请求模型"""
    user_id: int
    contents: List[Dict[str, Any]]


class OrganizeResponse(BaseModel):
    """健康档案整理响应模型"""
    record_id: str
    structured_text: str


class AnalyzeRequest(BaseModel):
    """健康档案分析请求模型"""
    user_id: int
    record_id: str


class AnalyzeResponse(BaseModel):
    """健康档案分析响应模型"""
    advice: str


class DataChangedWebhookRequest(BaseModel):
    """数据变化 Webhook 请求模型（已废弃，保留用于向后兼容）"""
    userId: int
    dataIds: List[int]
    latestUpdateTime: str


class HealthUpdateWebhookRequest(BaseModel):
    """健康数据更新 Webhook 请求模型"""
    userId: int
    event: str
    updateTime: str


class DataChangedWebhookResponse(BaseModel):
    """数据变化 Webhook 响应模型"""
    success: bool
    message: str


class RebuildRequest(BaseModel):
    """健康档案重建请求模型"""
    userId: int
    event: str
    updateTime: Optional[str] = None


class RebuildResponse(BaseModel):
    """健康档案重建响应模型"""
    code: int
    message: str
    data: Optional[Dict[str, Any]] = None


@router.post("/health-record/organize", response_model=OrganizeResponse)
async def organize_health_record(
    request: OrganizeRequest,
    llm: LlmClient = Depends(get_llm),
    storage: HealthRecordStorage = Depends(get_storage)
):
    """
    健康档案整理接口
    
    接收患者的多模态健康资料，整理生成结构化的健康档案文本。
    
    Args:
        request: 整理请求，包含用户ID和多模态内容
        llm: LLM 实例（依赖注入）
        storage: 健康档案存储实例（依赖注入）
        
    Returns:
        整理结果，包含记录ID和结构化文本
    """
    try:
        # 1. 构建系统提示词
        system_prompt = build_organize_prompt(request.contents)
        
        # 2. 构建多模态消息（contents 已经是多模态格式）
        messages = build_medical_messages(system_prompt, request.contents)
        
        # 3. 调用 LLM 生成结构化文本
        structured_text = llm.generate(
            messages,
            trace_user_id=request.user_id,
            trace_label="api_health_record_organize",
        )
        
        # 4. 生成记录ID
        record_id = str(uuid.uuid4())
        
        # 5. 保存到存储
        storage.save_record(
            user_id=request.user_id,
            record_id=record_id,
            content=structured_text
        )
        
        # 6. 返回结果
        return OrganizeResponse(
            record_id=record_id,
            structured_text=structured_text
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"处理请求时出错: {str(e)}")


@router.post("/health-record/analyze", response_model=AnalyzeResponse)
async def analyze_health_record(
    request: AnalyzeRequest,
    llm: LlmClient = Depends(get_llm),
    storage: HealthRecordStorage = Depends(get_storage)
):
    """
    健康档案分析接口
    
    基于已整理的健康档案，生成专业的医疗建议和分析。
    
    Args:
        request: 分析请求，包含用户ID和记录ID
        llm: LLM 实例（依赖注入）
        storage: 健康档案存储实例（依赖注入）
        
    Returns:
        分析结果，包含专业建议
        
    Raises:
        HTTPException: 当健康档案不存在时返回 404
    """
    try:
        # 1. 从存储中读取健康档案
        health_record = storage.get_record(
            user_id=request.user_id,
            record_id=request.record_id
        )
        
        # 2. 检查记录是否存在
        if health_record is None:
            raise HTTPException(
                status_code=404,
                detail=f"健康档案不存在：用户ID {request.user_id}，记录ID {request.record_id}"
            )
        
        # 3. 构建分析提示词
        prompt = build_analyze_prompt(health_record)
        
        # 4. 构建消息（纯文本，不需要多模态）
        messages = [
            {"role": "user", "content": prompt}
        ]
        
        # 5. 调用 LLM 生成分析建议
        advice = llm.generate(
            messages,
            trace_user_id=request.user_id,
            trace_label="api_health_record_analyze",
        )
        
        # 6. 返回结果
        return AnalyzeResponse(advice=advice)
    except HTTPException:
        # 重新抛出 HTTPException（如 404）
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"处理请求时出错: {str(e)}")


@router.post("/webhook/data-changed", response_model=DataChangedWebhookResponse)
async def webhook_data_changed(
    request: DataChangedWebhookRequest,
    storage: HealthRecordStorage = Depends(get_storage)
):
    """
    数据变化 Webhook 接口（已废弃，保留用于向后兼容）
    
    接收原始数据库变化通知，标记用户需要刷新健康档案。
    
    请求格式：
    {
        "userId": 2,
        "dataIds": [1, 2, 3],
        "latestUpdateTime": "2025-01-15T10:30:00"
    }
    
    Args:
        request: Webhook 请求，包含用户ID、数据ID列表和最新更新时间
        storage: 健康档案存储实例（依赖注入）
        
    Returns:
        Webhook 响应，包含成功状态和消息
    """
    try:
        logger.info(
            f"收到数据变化通知：userId={request.userId}, "
            f"dataIds={request.dataIds}, latestUpdateTime={request.latestUpdateTime}"
        )
        
        # 标记用户需要刷新健康档案
        storage.mark_needs_refresh(request.userId)
        
        logger.info(f"已标记用户 {request.userId} 需要刷新健康档案")
        
        return DataChangedWebhookResponse(
            success=True,
            message=f"用户 {request.userId} 的数据变化通知已接收"
        )
        
    except Exception as e:
        logger.error(f"处理数据变化通知时出错: {str(e)}", exc_info=True)
        return DataChangedWebhookResponse(
            success=False,
            message=f"处理通知时出错: {str(e)}"
        )


@router.post("/webhook/health-update", response_model=DataChangedWebhookResponse)
async def webhook_health_update(
    request: HealthUpdateWebhookRequest,
    orchestrator: ChatOrchestrator = Depends(get_orchestrator)
):
    """
    健康数据更新 Webhook 接口
    
    接收"原始数据已更新"的通知，触发本系统的健康档案重建流程。
    
    符合 outside/SYSTEM_BEHAVIOR.md 中的行为规范：
    - 触发方式：外部系统 POST /webhook/health-update
    - 读取原始数据（/raw-health-data/user/{userId}）
    - 不读健康档案（直接基于原始数据生成）
    - 写库（analysis / recommendation / archive 表）
    - 异步执行，不返回前端
    
    职责：
    - 接收健康数据更新事件（userId、event、updateTime）
    - 异步触发本系统的健康档案重建任务
    - 记录调用结果日志（成功/失败）
    - 不等待档案生成完成
    - 不做重试策略
    
    请求格式：
    {
        "userId": 2,
        "event": "data_updated",
        "updateTime": "2025-01-15T10:30:00"
    }
    
    Args:
        request: Webhook 请求，包含用户ID、事件类型和更新时间
        
    Returns:
        Webhook 响应，包含成功状态和消息
    """
    try:
        logger.info(
            f"[WEBHOOK] 收到健康数据更新通知：userId={request.userId}, "
            f"event={request.event}, updateTime={request.updateTime}"
        )
        
        if orchestrator is None:
            logger.error("[WEBHOOK] ChatOrchestrator 实例未初始化")
            return DataChangedWebhookResponse(
                success=False,
                message="系统错误：ChatOrchestrator 未初始化"
            )
        
        # 触发本系统的健康档案重建流程（异步执行）
        # 生成任务ID
        task_id = f"task_{datetime.now().timestamp()}_{uuid.uuid4().hex[:8]}"
        
        # 在后台异步执行重建任务
        asyncio.create_task(execute_rebuild_task(
            user_id=request.userId,
            event=request.event.strip(),
            update_time=request.updateTime,
            task_id=task_id,
            orchestrator=orchestrator
        ))
        
        logger.info(f"[WEBHOOK] 健康档案重建任务已提交: userId={request.userId}, taskId={task_id}, event={request.event}")
        
        return DataChangedWebhookResponse(
            success=True,
            message=f"用户 {request.userId} 的健康数据更新通知已接收，已触发健康档案重建任务（taskId: {task_id}）"
        )
        
    except Exception as e:
        logger.error(f"处理健康数据更新通知时出错: {str(e)}", exc_info=True)
        return DataChangedWebhookResponse(
            success=False,
            message=f"处理通知时出错: {str(e)}"
        )


async def execute_rebuild_task(user_id: int, event: str, update_time: Optional[str], task_id: str, orchestrator: ChatOrchestrator):
    """
    异步执行健康档案重建任务（webhook 触发的健康分析任务）
    
    符合 outside/SYSTEM_BEHAVIOR.md 中的行为规范：
    - 读取原始数据（/raw-health-data/user/{userId}）
    - 不读健康档案（直接基于原始数据生成）
    - 写库（analysis / recommendation / archive 表）
    - 异步执行，不返回前端
    
    Args:
        user_id: 用户ID
        event: 事件类型
        update_time: 更新时间
        task_id: 任务ID
        orchestrator: ChatOrchestrator 实例
    """
    try:
        logger.info(f"开始执行健康档案重建任务: userId={user_id}, taskId={task_id}, event={event}")
        
        # 1. 设置健康档案状态为 DIRTY（如果需要）
        # 注意：rebuild_health_archive 会检查状态，如果不是 DIRTY 会抛出异常
        # 这里我们需要先确保状态是 DIRTY
        storage = orchestrator.health_record_storage
        current_status = storage.get_archive_status(user_id=user_id, record_id="latest")
        
        if current_status != "DIRTY":
            if current_status is None:
                # 如果记录不存在，先创建一个空记录，然后设置为 DIRTY
                logger.info(f"用户 {user_id} 的健康档案不存在，创建初始记录并设置为 DIRTY")
                storage.save_record(
                    user_id=user_id,
                    record_id="latest",
                    content="{}"  # 创建空 JSON 记录
                )
                # 设置为 DIRTY 状态
                storage.set_archive_status(user_id=user_id, record_id="latest", status="DIRTY")
            else:
                # 如果状态不是 DIRTY，设置为 DIRTY
                logger.info(f"用户 {user_id} 的健康档案状态为 {current_status}，设置为 DIRTY")
                success = storage.set_archive_status(user_id=user_id, record_id="latest", status="DIRTY")
                if not success:
                    logger.warning(f"设置用户 {user_id} 的健康档案状态为 DIRTY 失败，记录可能不存在，尝试创建记录")
                    # 如果设置失败，尝试创建记录
                    storage.save_record(
                        user_id=user_id,
                        record_id="latest",
                        content="{}"
                    )
                    storage.set_archive_status(user_id=user_id, record_id="latest", status="DIRTY")
        
        # 2. 执行重建
        # 注意：rebuild_health_archive 会：
        # - 读取原始数据（fetch_user_health_data）
        # - 生成健康档案（generate_health_archive）
        # - 推送健康档案到外部数据库（push_medical_report）
        logger.info(f"[WEBHOOK任务] 步骤1：开始重建健康档案（读取原始数据，不读健康档案）")
        archive_json = orchestrator.rebuild_health_archive(user_id)
        logger.info(f"[WEBHOOK任务] 步骤1完成：健康档案已生成并推送到外部数据库")
        
        # 3. 获取必要的参数（archiveId 和 sourceRawId）
        # 等待健康档案推送到外部数据库后，获取最新的 archiveId
        archive_id = None
        source_raw_id = None
        
        try:
            # 获取最新的健康档案ID
            archive_id = get_latest_archive_id(user_id)
            if not archive_id:
                logger.warning(f"[WEBHOOK任务] 无法获取用户 {user_id} 的健康档案ID，将跳过健康分析和建议生成")
            else:
                logger.info(f"[WEBHOOK任务] 获取到健康档案ID: {archive_id}")
                
                # 获取原始数据ID（使用第一条数据的ID作为 sourceRawId）
                health_data_client = orchestrator.health_data_client
                records = health_data_client.fetch_user_health_data(user_id)
                if records and len(records) > 0:
                    source_raw_id = records[0].get("id")
                    if source_raw_id:
                        logger.info(f"[WEBHOOK任务] 获取到原始数据ID: {source_raw_id}")
                    else:
                        logger.warning(f"[WEBHOOK任务] 原始数据中没有ID字段，将使用默认值0")
                        source_raw_id = 0
                else:
                    logger.warning(f"[WEBHOOK任务] 用户 {user_id} 没有原始健康数据，将使用默认值0作为sourceRawId")
                    source_raw_id = 0
        except Exception as e:
            logger.error(f"[WEBHOOK任务] 获取 archiveId 或 sourceRawId 失败: {str(e)}", exc_info=True)
            logger.warning(f"[WEBHOOK任务] 将跳过健康分析和建议生成")
        
        # 4. 生成并提交健康分析结果
        analysis_result = None
        if archive_id is not None:
            try:
                logger.info(f"[WEBHOOK任务] 步骤2：开始生成健康分析结果")
                # 获取 LLM 实例
                llm = get_llm()
                if not llm:
                    logger.error(f"[WEBHOOK任务] LLM 实例未初始化，无法生成健康分析")
                else:
                    # 生成健康分析结果
                    analysis_result = generate_health_analysis(user_id, llm)
                    logger.info(f"[WEBHOOK任务] 步骤2完成：健康分析结果已生成")
                    
                    # 提交健康分析结果
                    logger.info(f"[WEBHOOK任务] 步骤3：开始提交健康分析结果")
                    analysis_payload = {
                        "archiveId": archive_id,
                        "sourceRawId": source_raw_id,
                        "modelName": settings.llm_model,
                        "modelVersion": "1.0",  # 根据实际版本调整
                        "analysisResult": analysis_result
                    }
                    submit_health_analysis(user_id, analysis_payload)
                    logger.info(f"[WEBHOOK任务] 步骤3完成：健康分析结果已提交到数据库")
            except Exception as e:
                logger.error(f"[WEBHOOK任务] 生成或提交健康分析结果失败: {str(e)}", exc_info=True)
                # 继续执行后续步骤，不中断流程
        
        # 5. 生成并提交健康建议
        if analysis_result is not None:
            try:
                logger.info(f"[WEBHOOK任务] 步骤4：开始生成健康建议")
                # 获取 LLM 实例
                llm = get_llm()
                if not llm:
                    logger.error(f"[WEBHOOK任务] LLM 实例未初始化，无法生成健康建议")
                else:
                    # 生成健康建议（基于已生成的分析结果）
                    recommendation_result = generate_health_recommendation(user_id, analysis_result=analysis_result)
                    logger.info(f"[WEBHOOK任务] 步骤4完成：健康建议已生成")
                    
                    # 提交健康建议
                    logger.info(f"[WEBHOOK任务] 步骤5：开始提交健康建议")
                    # 转换建议格式
                    recommendations = convert_recommendation_result_to_api_format(recommendation_result)
                    
                    recommendation_payload = {
                        "archiveId": archive_id,
                        "sourceRawId": source_raw_id,
                        "modelName": settings.llm_model,
                        "modelVersion": "1.0",  # 根据实际版本调整
                        "analysisResult": analysis_result,
                        "recommendations": recommendations
                    }
                    submit_health_recommendation(user_id, recommendation_payload)
                    logger.info(f"[WEBHOOK任务] 步骤5完成：健康建议已提交到数据库")
            except Exception as e:
                logger.error(f"[WEBHOOK任务] 生成或提交健康建议失败: {str(e)}", exc_info=True)
                # 继续执行，不中断流程
        
        logger.info(f"[WEBHOOK任务] 所有任务完成: userId={user_id}, taskId={task_id}")
        
    except Exception as e:
        logger.error(f"健康档案重建失败: userId={user_id}, taskId={task_id}, error={str(e)}", exc_info=True)
        # 失败时状态已经在 rebuild_health_archive 中恢复为 DIRTY


@router.post("/health-archive-process/rebuild", response_model=RebuildResponse)
async def rebuild_health_archive(
    request: RebuildRequest,
    orchestrator: ChatOrchestrator = Depends(get_orchestrator)
):
    """
    触发健康档案重建接口
    
    接收重建请求，异步提交重建任务，立即返回，不等待完成。
    
    请求格式：
    {
        "userId": 2,
        "event": "data_updated",
        "updateTime": "2026-01-27T13:54:30.9489043"
    }
    
    Args:
        request: 重建请求，包含用户ID、事件类型和更新时间
        orchestrator: ChatOrchestrator 实例（依赖注入）
        
    Returns:
        重建响应，包含任务ID和状态
    """
    try:
        # 1. 参数验证
        if request.userId <= 0:
            raise HTTPException(
                status_code=400,
                detail="用户ID无效"
            )
        
        if not request.event or not request.event.strip():
            raise HTTPException(
                status_code=400,
                detail="事件类型无效"
            )
        
        # 2. 生成任务ID
        task_id = f"task_{datetime.now().timestamp()}_{uuid.uuid4().hex[:8]}"
        
        # 3. 在后台异步执行重建任务
        asyncio.create_task(execute_rebuild_task(
            user_id=request.userId,
            event=request.event.strip(),
            update_time=request.updateTime,
            task_id=task_id,
            orchestrator=orchestrator
        ))
        
        logger.info(f"健康档案重建任务已提交: userId={request.userId}, taskId={task_id}, event={request.event}")
        
        # 4. 立即返回（不等待完成）
        return RebuildResponse(
            code=200,
            message="健康档案重建任务已提交",
            data={
                "taskId": task_id,
                "userId": request.userId,
                "status": "processing"
            }
        )
        
    except HTTPException:
        # 重新抛出 HTTPException（如 400）
        raise
    except Exception as e:
        logger.error(f"处理健康档案重建请求时出错: {str(e)}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"处理请求时出错: {str(e)}"
        )
