# -*- coding: utf-8 -*-

import json
import os
import subprocess
from contextlib import asynccontextmanager
from typing import Optional, List, Dict, Any
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
import uvicorn

import config


@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期：启动时加载索引，关闭时清理。"""
    try:
        from rag_engine import load_index
        load_index()
    except FileNotFoundError as e:
        print("启动提示:", e)
        if getattr(config, "RAG_INDEX_SOURCE", "") == "english_bge":
            print("请先执行: python build_index_en_chroma.py")
        else:
            print("请先执行: python build_index_ch_chroma.py")
    yield
    # shutdown 可在此处添加清理逻辑


app = FastAPI(
    title="RAG AI 医生",
    description="基于 Hugging Face HuatuoGPT + ChatDoctor 数据集的 RAG 医学问答",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


class ChatMessage(BaseModel):
    role: str  # "user" | "assistant"
    content: str


class ChatRequest(BaseModel):
    query: str
    top_k: Optional[int] = None
    use_llm: Optional[str] = None  # "ollama" | "huatuo" | "small" | "none"
    history: Optional[List[ChatMessage]] = None  # 上下文对话记录


class ChatResponse(BaseModel):
    answer: str
    references: list
    used_llm: bool
    best_similarity: Optional[float] = None


class RebuildVectorRequest(BaseModel):
    dataset: str  # qa|liver|llama|drug|english|all
    dbHost: str
    dbPort: int
    dbName: str
    dbUser: str
    dbPassword: str


@app.get("/health")
def health():
    return {
        "status": "ok",
        "service": "ai-doctor-rag",
        "rag_index_source": getattr(config, "RAG_INDEX_SOURCE", ""),
        "port": config.PORT,
    }


@app.post("/internal/reload-rag-index")
def reload_rag_index():
    """管理端重建向量库后调用，使当前进程重新挂载 Chroma 集合（中/英取决于 RAG_INDEX_SOURCE）。"""
    try:
        src = getattr(config, "RAG_INDEX_SOURCE", "")
        if src == "english_bge":
            from rag_engine import reload_english_bge_index
            reload_english_bge_index()
        else:
            from rag_engine import reload_chinese_bge_index
            reload_chinese_bge_index()
        return {"ok": True}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/internal/rebuild-vector")
def rebuild_vector(req: RebuildVectorRequest):
    """由管理端调用：按数据集重建 Chroma，并在完成后热加载。"""
    ds = (req.dataset or "").strip()
    if ds not in {"qa", "liver", "llama", "drug", "english", "all"}:
        raise HTTPException(status_code=400, detail="dataset 必须是 qa|liver|llama|drug|english|all")

    script = os.path.join(os.path.dirname(os.path.abspath(__file__)), "scripts", "rebuild_chroma_from_mysql.py")
    if not os.path.isfile(script):
        raise HTTPException(status_code=500, detail=f"脚本不存在: {script}")

    python_exec = os.environ.get("AI_DOCTOR_PYTHON_EXECUTABLE", "python")
    cmd = [
        python_exec,
        script,
        "--dataset",
        ds,
        "--host",
        req.dbHost,
        "--port",
        str(req.dbPort),
        "--database",
        req.dbName,
        "--user",
        req.dbUser,
        "--password",
        req.dbPassword,
    ]
    try:
        out = subprocess.run(cmd, capture_output=True, text=True, check=True, timeout=60 * 60)
        try:
            from rag_engine import reload_chinese_bge_index
            reload_chinese_bge_index()
        except Exception:
            pass
        return {"ok": True, "message": "向量重建完成", "stdout": out.stdout[-2000:], "stderr": out.stderr[-2000:]}
    except subprocess.CalledProcessError as e:
        raise HTTPException(status_code=500, detail=f"重建失败: {e.stderr[-2000:] or e.stdout[-2000:]}")
    except subprocess.TimeoutExpired:
        raise HTTPException(status_code=500, detail="重建超时")


@app.post("/chat", response_model=ChatResponse)
def chat(req: ChatRequest):
    if not req.query.strip():
        raise HTTPException(status_code=400, detail="query 不能为空")
    try:
        from rag_engine import chat
        out = chat(
            query=req.query,
            k=req.top_k,
            use_llm=req.use_llm,
            history=[{"role": m.role, "content": m.content} for m in (req.history or [])],
        )
        return ChatResponse(
            answer=out["answer"],
            references=out["references"],
            used_llm=out["used_llm"],
            best_similarity=out.get("best_similarity"),
        )
    except FileNotFoundError as e:
        raise HTTPException(status_code=503, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


def _chat_stream_gen(query: str, top_k: Optional[int], use_llm: Optional[str], history: Optional[List[Dict[str, Any]]] = None):
    """流式生成器：每行一个 JSON，type 为 meta | chunk 文本 | done"""
    from rag_engine import chat_stream
    for item in chat_stream(query=query, k=top_k, use_llm=use_llm, history=history):
        if isinstance(item, dict):
            yield json.dumps(item, ensure_ascii=False) + "\n"
        else:
            yield json.dumps({"type": "chunk", "content": item}, ensure_ascii=False) + "\n"


@app.post("/chat/stream")
def chat_stream(req: ChatRequest):
    """流式对话接口，返回 NDJSON（每行一个 JSON 对象）。"""
    if not req.query.strip():
        raise HTTPException(status_code=400, detail="query 不能为空")
    try:
        return StreamingResponse(
            _chat_stream_gen(
                req.query, req.top_k, req.use_llm,
                history=[{"role": m.role, "content": m.content} for m in (req.history or [])],
            ),
            media_type="application/x-ndjson",
            headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
        )
    except FileNotFoundError as e:
        raise HTTPException(status_code=503, detail=str(e))


@app.get("/")
def root():
    return {
        "message": "RAG AI 医生 API",
        "docs": "/docs",
        "chat": "POST /chat",
        "chat_stream": "POST /chat/stream",
        "health": "GET /health",
    }


if __name__ == "__main__":
    uvicorn.run(
        "app:app",
        host=config.HOST,
        port=config.PORT,
        reload=False,
    )
