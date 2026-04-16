"""
健康档案处理系统主入口（已移除对话功能）。
仅保留健康档案写入/重建、健康总结与健康建议相关接口。
"""
import asyncio
import logging

import uvicorn
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from app.core.config import settings
from app.core.health_record_storage import HealthRecordStorage
from app.core.llm_client import LlmClient
from app.api import routes
from app.api.routes import RebuildRequest, RebuildResponse

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)
logging.getLogger("ai_agent.pipeline").setLevel(logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    debug=settings.debug,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 保留 LLM 与 orchestrator，仅用于健康档案重建/分析/建议流水线
logger.info("初始化健康档案处理依赖")
llm = LlmClient()
from app.services.chat_orchestrator import ChatOrchestrator

_shared_storage = HealthRecordStorage()
orchestrator = ChatOrchestrator(llm, health_record_storage=_shared_storage)

routes._llm = llm
routes._orchestrator = orchestrator
routes._storage = _shared_storage

app.include_router(routes.router, prefix=settings.api_prefix)


@app.post("/webhook/archive-created")
async def webhook_archive_created(request: dict):
    """健康档案创建后触发：生成健康总结与建议并写回后端。"""
    try:
        user_id = request.get("userId")
        archive_id = request.get("archiveId")
        if not user_id or not archive_id:
            raise HTTPException(status_code=400, detail="userId 和 archiveId 必填")
        from app.api.archive_created_handler import execute_archive_created_task
        asyncio.create_task(execute_archive_created_task(
            user_id=int(user_id),
            archive_id=int(archive_id),
            orchestrator=routes._orchestrator
        ))
        logger.info(f"[WEBHOOK] 健康档案创建通知已接收：userId={user_id}, archiveId={archive_id}")
        return {"code": 200, "message": "通知已接收", "success": True}
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"处理 archive-created webhook 失败: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/health-archive-process/rebuild", response_model=RebuildResponse)
async def rebuild_health_archive_root(request: RebuildRequest):
    """根路径兼容接口：异步触发健康档案重建并写入总结建议。"""
    if routes._orchestrator is None:
        raise HTTPException(status_code=500, detail="ChatOrchestrator 未初始化")
    from app.api.routes import rebuild_health_archive
    return await rebuild_health_archive(request=request, orchestrator=routes._orchestrator)


@app.get("/")
async def root():
    return {
        "message": "健康档案处理系统 API（已移除对话能力）",
        "version": settings.app_version,
        "docs": "/docs",
        "api_prefix": settings.api_prefix,
    }


@app.get("/health")
async def health():
    return {"status": "ok"}


if __name__ == "__main__":
    # 直接传入 app，避免 uvicorn 再 import "main:app" 导致顶层初始化跑两遍
    if settings.debug:
        uvicorn.run(
            "main:app",
            host="0.0.0.0",
            port=8000,
            reload=True,
        )
    else:
        uvicorn.run(app, host="0.0.0.0", port=8000, reload=False)













































