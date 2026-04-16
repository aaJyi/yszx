from __future__ import annotations

import json
import logging
import threading
import uuid
from typing import Any

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

from app.config import settings
from app.db import (
    count_open_tasks,
    get_artifact,
    get_run,
    insert_run,
    insert_task,
    insert_artifact,
    list_artifacts,
    list_tasks,
    update_run_plan,
    update_run_status,
    update_task_output,
    update_task_status,
)
from app.llm import LLMError, ollama_chat_json, ollama_chat_text
from app.search import SearchError, web_search_duckduckgo

logger = logging.getLogger(__name__)


app = FastAPI(title="Smart-Assistant API", version="0.1.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


class CreateRunRequest(BaseModel):
    prompt: str = Field(min_length=1, max_length=8000)


def _save_artifact(run_id: str, kind: str, name: str, content: str, content_type: str) -> str:
    artifact_id = str(uuid.uuid4())
    insert_artifact(run_id, artifact_id, kind, name, content, content_type)
    return artifact_id


PLANNER_SYSTEM = """你是任务规划智能体（Planner）。
你必须把用户的大目标拆成可并行/可串行执行的子任务，并输出严格 JSON 数组。
每个元素必须包含字段：
- id: 从 1 开始的整数
- task: 简短可执行任务描述（中文）
- task_type: 只能是 research | outline | write | ppt | synth
- depends_on: 整数数组，表示依赖哪些 id（没有依赖则 []）

只输出 JSON，不要输出任何额外文本。"""

PLANNER_SCHEMA_HINT = """JSON 形状示例：
[
  {"id": 1, "task": "...", "task_type": "research", "depends_on": []},
  {"id": 2, "task": "...", "task_type": "outline", "depends_on": [1]}
]"""


def _fallback_plan(prompt: str) -> list[dict[str, Any]]:
    return [
        {"id": 1, "task": f"联网收集与“{prompt}”相关的高质量资料与要点，并给出链接", "task_type": "research", "depends_on": []},
        {"id": 2, "task": "整理综述结构与章节大纲（含每章要点）", "task_type": "outline", "depends_on": [1]},
        {"id": 3, "task": "撰写综述正文（含引用/链接列表）", "task_type": "write", "depends_on": [2]},
        {"id": 4, "task": "生成PPT逐页内容（JSON：title + slides[]）", "task_type": "ppt", "depends_on": [3]},
        {"id": 5, "task": "汇总最终交付物：文章 + PPT JSON，并做一致性检查", "task_type": "synth", "depends_on": [4]},
    ]


def _run_research(prompt: str) -> dict[str, Any]:
    q = f"{prompt} 综述 关键进展 论文"
    results = web_search_duckduckgo(q, max_results=6)
    return {"query": q, "results": results}


def _run_outline(prompt: str, research: Any | None) -> str:
    refs = json.dumps(research, ensure_ascii=False) if research is not None else ""
    system = "你是学术写作助手，负责产出清晰的综述结构大纲（中文）。"
    user = f"""用户主题：{prompt}

参考检索结果（可能为空）：{refs}

请输出：
1) 综述标题建议（1-3个）
2) 章节结构（不少于6章，每章3-6条要点）
3) 关键术语表（10-20个）
"""
    return ollama_chat_text(system, user)


def _run_write(prompt: str, outline: Any | None, research: Any | None) -> str:
    outline_text = outline if isinstance(outline, str) else json.dumps(outline, ensure_ascii=False)
    research_text = json.dumps(research, ensure_ascii=False) if research is not None else ""
    system = "你是严谨的学术写作助手，用中文撰写综述，避免编造具体论文细节；可以引用提供的链接作为参考。"
    user = f"""主题：{prompt}

大纲：{outline_text}

检索结果：{research_text}

要求：
- 正文结构与大纲一致
- 适合“综述”文体，强调背景、方法、数据/评测、应用、挑战与展望
- 末尾给出“参考链接”列表（使用检索结果中的 url）
"""
    return ollama_chat_text(system, user)


def _run_ppt(prompt: str, article: str) -> dict[str, Any]:
    system = "你是PPT生成助手，把文章转成逐页PPT内容，输出严格JSON。"
    user = f"""请把下面文章转换成PPT逐页内容，返回严格JSON，结构为：
{{
  "title": "...",
  "slides": [
    {{"title": "...", "bullets": ["...","..."], "speaker_notes": "..."}}
  ]
}}

要求：
- 10~14页
- 要点简洁
- 不要输出任何额外文本，只输出JSON

文章：
{article}
"""
    txt = ollama_chat_text(system, user)
    try:
        return json.loads(txt)
    except Exception:
        return {"title": f"{prompt}（PPT）", "slides_raw": txt}


def _run_synth(prompt: str, outline: Any | None, article: Any | None, ppt: Any | None) -> dict[str, Any]:
    system = "你是汇总与质检助手，输出最终交付物的摘要与结构化索引（中文）。"
    user = f"""主题：{prompt}

请输出一个JSON，包含：
- "summary": 对最终交付物做 5~10 条要点总结
- "deliverables": 列出产物名称与简短说明（文章、PPT等）
- "notes": 风险/不确定性（如引用可靠性、需要人工核验点）

大纲：
{outline if isinstance(outline, str) else json.dumps(outline, ensure_ascii=False)}

文章：
{article if isinstance(article, str) else json.dumps(article, ensure_ascii=False)}

PPT：
{json.dumps(ppt, ensure_ascii=False) if ppt is not None else ""}

只输出JSON，不要输出其他文本。
"""
    txt = ollama_chat_text(system, user)
    try:
        return json.loads(txt)
    except Exception:
        return {"summary": [], "deliverables": [], "notes": txt}


def _execute_plan_item(prompt: str, item: dict[str, Any], seq_to_output: dict[int, Any]) -> Any:
    ttype = str(item.get("task_type", "")).strip()
    deps = [int(d) for d in (item.get("depends_on", []) or []) if str(d).isdigit()]
    dep_outputs = [seq_to_output[d] for d in deps if d in seq_to_output]

    if ttype == "research":
        return _run_research(prompt)
    if ttype == "outline":
        # 优先使用依赖结果；若无依赖则回退到默认 research 槽位
        research_ctx = dep_outputs[0] if dep_outputs else seq_to_output.get(1)
        return _run_outline(prompt, research_ctx)
    if ttype == "write":
        # 约定：第一个依赖一般是 outline，第二个依赖一般是 research
        outline_ctx = dep_outputs[0] if len(dep_outputs) >= 1 else seq_to_output.get(2)
        research_ctx = dep_outputs[1] if len(dep_outputs) >= 2 else seq_to_output.get(1)
        return _run_write(prompt, outline_ctx, research_ctx)
    if ttype == "ppt":
        article_ctx = dep_outputs[0] if dep_outputs else seq_to_output.get(3)
        article_text = article_ctx if isinstance(article_ctx, str) else json.dumps(article_ctx, ensure_ascii=False)
        return _run_ppt(prompt, article_text)
    if ttype == "synth":
        outline_ctx = dep_outputs[0] if len(dep_outputs) >= 1 else seq_to_output.get(2)
        article_ctx = dep_outputs[1] if len(dep_outputs) >= 2 else seq_to_output.get(3)
        ppt_ctx = dep_outputs[2] if len(dep_outputs) >= 3 else seq_to_output.get(4)
        return _run_synth(prompt, outline_ctx, article_ctx, ppt_ctx)
    return {"note": f"unknown task_type={ttype}", "task": item.get("task")}


def _execute_run_pipeline(
    run_id: str,
    prompt: str,
    plan: list[dict[str, Any]],
    seq_to_task_id: dict[int, str],
):
    seq_to_output: dict[int, Any] = {}
    pending_items: dict[int, dict[str, Any]] = {int(item.get("id")): item for item in plan if item.get("id") is not None}
    done_seqs: set[int] = set()
    failed_seqs: set[int] = set()
    executed_any = True

    try:
        while pending_items and executed_any:
            executed_any = False
            current_batch: list[dict[str, Any]] = []

            for seq, item in sorted(pending_items.items(), key=lambda kv: kv[0]):
                depends_on = [int(d) for d in (item.get("depends_on", []) or [])]
                if all(d in done_seqs for d in depends_on):
                    current_batch.append(item)

            for item in current_batch:
                seq = int(item.get("id"))
                task_id = seq_to_task_id.get(seq)
                if not task_id:
                    pending_items.pop(seq, None)
                    continue

                update_task_status(task_id, "running")
                try:
                    out = _execute_plan_item(prompt, item, seq_to_output)
                    update_task_output(task_id, out)
                    seq_to_output[seq] = out
                    done_seqs.add(seq)
                except Exception as e:
                    update_task_status(task_id, "failed", str(e))
                    failed_seqs.add(seq)
                finally:
                    pending_items.pop(seq, None)
                    executed_any = True

        # 剩余不可执行任务：依赖失败或循环依赖
        for seq, item in sorted(pending_items.items(), key=lambda kv: kv[0]):
            task_id = seq_to_task_id.get(seq)
            if task_id:
                depends_on = [int(d) for d in (item.get("depends_on", []) or [])]
                dep_failed = [d for d in depends_on if d in failed_seqs]
                if dep_failed:
                    update_task_status(task_id, "failed", f"依赖任务失败: {dep_failed}")
                else:
                    update_task_status(task_id, "failed", "任务依赖不可达（可能存在循环依赖）")
            failed_seqs.add(seq)

        # 生成 artifacts
        if 2 in seq_to_output:
            _save_artifact(run_id, "text", "outline.md", str(seq_to_output[2]), "text/markdown")
        if 3 in seq_to_output:
            _save_artifact(run_id, "text", "article.md", str(seq_to_output[3]), "text/markdown")
        if 4 in seq_to_output:
            _save_artifact(run_id, "json", "ppt.json", json.dumps(seq_to_output[4], ensure_ascii=False), "application/json")
        if 5 in seq_to_output:
            _save_artifact(run_id, "json", "final.json", json.dumps(seq_to_output[5], ensure_ascii=False), "application/json")

        if failed_seqs:
            update_run_status(run_id, "partial_done")
        else:
            update_run_status(run_id, "done")
    except Exception as e:
        logger.exception("run pipeline failed, run_id=%s", run_id)
        update_run_status(run_id, "failed")
        # 兜底：若有任务仍是 running，标记失败
        for task_id in seq_to_task_id.values():
            update_task_status(task_id, "failed", f"pipeline异常: {e}")


@app.post("/api/runs")
def create_run(req: CreateRunRequest):
    run_id = str(uuid.uuid4())
    insert_run(run_id=run_id, prompt=req.prompt, status="planning", plan=None)

    try:
        plan = ollama_chat_json(
            system=PLANNER_SYSTEM,
            user=f"用户需求：{req.prompt}",
            schema_hint=PLANNER_SCHEMA_HINT,
        )
        if not isinstance(plan, list) or not plan:
            raise ValueError("plan not list")
    except Exception as e:
        # 规划不稳定时，使用兜底计划，保证系统可跑通
        plan = _fallback_plan(req.prompt)
        update_run_plan(run_id, plan)
        update_run_status(run_id, "planned")
    else:
        update_run_plan(run_id, plan)
        update_run_status(run_id, "planned")

    # 写入 tasks 后后台执行，接口立即返回 run_id，避免前端长时间等待超时
    update_run_status(run_id, "running")
    seq_to_task_id: dict[int, str] = {}

    # 先建任务记录
    for item in plan:
        task_uuid = str(uuid.uuid4())
        seq = int(item.get("id"))
        task_name = str(item.get("task", "")).strip()[:255] or f"task-{seq}"
        task_type = str(item.get("task_type", "write")).strip()
        depends_on = item.get("depends_on", [])

        insert_task(
            task_id=task_uuid,
            run_id=run_id,
            seq=seq,
            task_name=task_name,
            task_type=task_type,
            depends_on=depends_on,
            status="pending",
            input_json={"prompt": req.prompt, "plan_item": item},
        )
        seq_to_task_id[seq] = task_uuid

    t = threading.Thread(
        target=_execute_run_pipeline,
        args=(run_id, req.prompt, plan, seq_to_task_id),
        daemon=True,
    )
    t.start()

    return {"run_id": run_id, "plan": plan, "status": "running"}


@app.get("/api/runs/{run_id}")
def get_run_detail(run_id: str):
    run = get_run(run_id)
    if not run:
        raise HTTPException(status_code=404, detail="run not found")
    tasks = list_tasks(run_id)
    artifacts = list_artifacts(run_id)

    # 若没有未完成任务，自动把 run 标成 done（worker 也会尝试标记）
    if run.get("status") == "running" and count_open_tasks(run_id) == 0:
        update_run_status(run_id, "done")
        run = get_run(run_id) or run

    return {"run": run, "tasks": tasks, "artifacts": artifacts}


@app.get("/api/runs/{run_id}/artifacts/{artifact_id}")
def get_artifact_detail(run_id: str, artifact_id: str):
    a = get_artifact(run_id, artifact_id)
    if not a:
        raise HTTPException(status_code=404, detail="artifact not found")
    return a


@app.get("/health")
def health():
    return {"ok": True, "ollama_model": settings.ollama_model}

