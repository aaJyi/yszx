from __future__ import annotations

import json
import time
import uuid
from typing import Any

import pika

from app.config import settings
from app.db import (
    count_open_tasks,
    get_run,
    get_task,
    insert_artifact,
    list_tasks,
    update_run_status,
    update_task_output,
    update_task_status,
)
from app.llm import LLMError, ollama_chat_text
from app.search import SearchError, web_search_duckduckgo


def _rabbitmq_connection() -> pika.BlockingConnection:
    creds = pika.PlainCredentials(settings.rabbitmq_user, settings.rabbitmq_password)
    params = pika.ConnectionParameters(
        host=settings.rabbitmq_host,
        port=settings.rabbitmq_port,
        credentials=creds,
        heartbeat=60,
        blocked_connection_timeout=60,
    )
    return pika.BlockingConnection(params)


def _deps_done(run_id: str, depends_on: list[int]) -> bool:
    if not depends_on:
        return True
    tasks = list_tasks(run_id)
    done_seqs = {int(t["seq"]) for t in tasks if t.get("status") == "done"}
    return all(int(d) in done_seqs for d in depends_on)


def _get_task_output_by_seq(run_id: str, seq: int) -> Any | None:
    for t in list_tasks(run_id):
        if int(t["seq"]) == int(seq):
            return t.get("output_json")
    return None


def _run_research(prompt: str) -> dict[str, Any]:
    q = f"{prompt} 综述 关键进展 论文"
    results = web_search_duckduckgo(q, max_results=6)
    return {"query": q, "results": results}


def _run_outline(prompt: str, research: Any | None) -> str:
    refs = ""
    if isinstance(research, dict):
        refs = json.dumps(research, ensure_ascii=False)
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
        # LLM 可能不严格，兜底为“半结构化”
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


def _maybe_finalize_run(run_id: str):
    if count_open_tasks(run_id) != 0:
        return
    update_run_status(run_id, "done")


def _save_artifacts(run_id: str):
    tasks = list_tasks(run_id)
    by_type = {t["task_type"]: t for t in tasks if t.get("status") == "done"}

    def _upsert(kind: str, name: str, content: str, content_type: str):
        artifact_id = str(uuid.uuid4())
        insert_artifact(run_id, artifact_id, kind, name, content, content_type)

    if "outline" in by_type:
        _upsert("text", "outline.md", str(by_type["outline"].get("output_json", "")), "text/markdown")
    if "write" in by_type:
        _upsert("text", "article.md", str(by_type["write"].get("output_json", "")), "text/markdown")
    if "ppt" in by_type:
        _upsert("json", "ppt.json", json.dumps(by_type["ppt"].get("output_json", {}), ensure_ascii=False), "application/json")
    if "synth" in by_type:
        _upsert("json", "final.json", json.dumps(by_type["synth"].get("output_json", {}), ensure_ascii=False), "application/json")


def handle_message(body: bytes):
    msg = json.loads(body.decode("utf-8"))
    task_id = msg["task_id"]
    run_id = msg["run_id"]

    task = get_task(task_id)
    if not task:
        return
    run = get_run(run_id)
    if not run:
        update_task_status(task_id, "failed", "run not found")
        return

    depends_on = task.get("depends_on_json") or []
    if not isinstance(depends_on, list):
        depends_on = []

    # 简单依赖等待：依赖没完成则回队列稍后重试
    if not _deps_done(run_id, depends_on):
        raise RuntimeError("deps not ready")

    prompt = (task.get("input_json") or {}).get("prompt") or run.get("prompt") or ""
    update_task_status(task_id, "running")

    try:
        ttype = task.get("task_type")
        if ttype == "research":
            out = _run_research(prompt)
        elif ttype == "outline":
            research = _get_task_output_by_seq(run_id, 1)
            out = _run_outline(prompt, research)
        elif ttype == "write":
            research = _get_task_output_by_seq(run_id, 1)
            outline = _get_task_output_by_seq(run_id, 2)
            out = _run_write(prompt, outline, research)
        elif ttype == "ppt":
            article = _get_task_output_by_seq(run_id, 3)
            out = _run_ppt(prompt, article if isinstance(article, str) else json.dumps(article, ensure_ascii=False))
        elif ttype == "synth":
            outline = _get_task_output_by_seq(run_id, 2)
            article = _get_task_output_by_seq(run_id, 3)
            ppt = _get_task_output_by_seq(run_id, 4)
            out = _run_synth(prompt, outline, article, ppt)
        else:
            out = {"note": f"unknown task_type={ttype}", "task": task.get("task_name")}

        update_task_output(task_id, out)

        # 如果最后一个任务完成或所有任务完成，则保存 artifacts
        if count_open_tasks(run_id) == 0:
            _save_artifacts(run_id)
            _maybe_finalize_run(run_id)

    except Exception as e:
        update_task_status(task_id, "failed", str(e))
        raise


def main():
    conn = _rabbitmq_connection()
    ch = conn.channel()
    ch.queue_declare(queue=settings.rabbitmq_queue, durable=True)
    ch.basic_qos(prefetch_count=1)

    def _callback(channel, method, properties, body: bytes):
        try:
            handle_message(body)
        except RuntimeError as e:
            # deps not ready：短暂等待后重新入队（简单重试）
            time.sleep(2)
            channel.basic_nack(delivery_tag=method.delivery_tag, requeue=True)
            return
        except Exception:
            channel.basic_nack(delivery_tag=method.delivery_tag, requeue=True)
            return
        channel.basic_ack(delivery_tag=method.delivery_tag)

    ch.basic_consume(queue=settings.rabbitmq_queue, on_message_callback=_callback, auto_ack=False)
    print(f"[worker] consuming queue={settings.rabbitmq_queue} on {settings.rabbitmq_host}:{settings.rabbitmq_port}")
    ch.start_consuming()


if __name__ == "__main__":
    main()

