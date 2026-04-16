import json
from contextlib import contextmanager
from typing import Any

import pymysql

from app.config import settings


def _conn():
    try:
        return pymysql.connect(
            host=settings.mysql_host,
            port=settings.mysql_port,
            user=settings.mysql_user,
            password=settings.mysql_password,
            database=settings.mysql_db,
            charset="utf8mb4",
            cursorclass=pymysql.cursors.DictCursor,
            autocommit=True,
        )
    except RuntimeError as e:
        msg = str(e)
        if "cryptography" in msg and (
            "caching_sha2" in msg.lower() or "sha256_password" in msg.lower()
        ):
            raise RuntimeError(
                "MySQL 使用 caching_sha2_password / sha256_password 时，PyMySQL 需要安装 cryptography。\n"
                "请用「与 uvicorn 相同的 Python」执行：\n"
                "  python -m pip install cryptography\n"
                "或把 MySQL 用户改为 mysql_native_password（见 README「常见报错」）。"
            ) from e
        raise


@contextmanager
def get_cursor():
    conn = _conn()
    try:
        with conn.cursor() as cur:
            yield cur
    finally:
        conn.close()


def insert_run(run_id: str, prompt: str, status: str, plan: Any | None):
    with get_cursor() as cur:
        cur.execute(
            "INSERT INTO runs (id, prompt, status, plan_json) VALUES (%s, %s, %s, %s)",
            (run_id, prompt, status, json.dumps(plan, ensure_ascii=False) if plan is not None else None),
        )


def update_run_status(run_id: str, status: str):
    with get_cursor() as cur:
        cur.execute("UPDATE runs SET status=%s WHERE id=%s", (status, run_id))


def update_run_plan(run_id: str, plan: Any):
    with get_cursor() as cur:
        cur.execute("UPDATE runs SET plan_json=%s WHERE id=%s", (json.dumps(plan, ensure_ascii=False), run_id))


def get_run(run_id: str) -> dict[str, Any] | None:
    with get_cursor() as cur:
        cur.execute("SELECT * FROM runs WHERE id=%s", (run_id,))
        row = cur.fetchone()
        if not row:
            return None
        if row.get("plan_json") is not None and isinstance(row["plan_json"], str):
            try:
                row["plan_json"] = json.loads(row["plan_json"])
            except Exception:
                pass
        return row


def insert_task(
    task_id: str,
    run_id: str,
    seq: int,
    task_name: str,
    task_type: str,
    depends_on: Any | None,
    status: str,
    input_json: Any | None,
):
    with get_cursor() as cur:
        cur.execute(
            """
            INSERT INTO tasks (id, run_id, seq, task_name, task_type, depends_on_json, status, input_json)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
            """,
            (
                task_id,
                run_id,
                seq,
                task_name,
                task_type,
                json.dumps(depends_on, ensure_ascii=False) if depends_on is not None else None,
                status,
                json.dumps(input_json, ensure_ascii=False) if input_json is not None else None,
            ),
        )


def list_tasks(run_id: str) -> list[dict[str, Any]]:
    with get_cursor() as cur:
        cur.execute("SELECT * FROM tasks WHERE run_id=%s ORDER BY seq ASC", (run_id,))
        rows = cur.fetchall() or []
        for r in rows:
            for k in ("depends_on_json", "input_json", "output_json"):
                if r.get(k) is not None and isinstance(r[k], str):
                    try:
                        r[k] = json.loads(r[k])
                    except Exception:
                        pass
        return rows


def get_task(task_id: str) -> dict[str, Any] | None:
    with get_cursor() as cur:
        cur.execute("SELECT * FROM tasks WHERE id=%s", (task_id,))
        r = cur.fetchone()
        if not r:
            return None
        for k in ("depends_on_json", "input_json", "output_json"):
            if r.get(k) is not None and isinstance(r[k], str):
                try:
                    r[k] = json.loads(r[k])
                except Exception:
                    pass
        return r


def update_task_status(task_id: str, status: str, error: str | None = None):
    with get_cursor() as cur:
        cur.execute("UPDATE tasks SET status=%s, error=%s WHERE id=%s", (status, error, task_id))


def update_task_output(task_id: str, output: Any):
    with get_cursor() as cur:
        cur.execute(
            "UPDATE tasks SET output_json=%s, status=%s WHERE id=%s",
            (json.dumps(output, ensure_ascii=False), "done", task_id),
        )


def insert_artifact(run_id: str, artifact_id: str, kind: str, name: str, content: str, content_type: str):
    with get_cursor() as cur:
        cur.execute(
            "INSERT INTO artifacts (id, run_id, kind, name, content, content_type) VALUES (%s, %s, %s, %s, %s, %s)",
            (artifact_id, run_id, kind, name, content, content_type),
        )


def list_artifacts(run_id: str) -> list[dict[str, Any]]:
    with get_cursor() as cur:
        cur.execute("SELECT id, run_id, kind, name, content_type, created_at FROM artifacts WHERE run_id=%s", (run_id,))
        return cur.fetchall() or []


def get_artifact(run_id: str, artifact_id: str) -> dict[str, Any] | None:
    with get_cursor() as cur:
        cur.execute("SELECT * FROM artifacts WHERE run_id=%s AND id=%s", (run_id, artifact_id))
        return cur.fetchone()


def count_open_tasks(run_id: str) -> int:
    with get_cursor() as cur:
        cur.execute(
            "SELECT COUNT(1) AS c FROM tasks WHERE run_id=%s AND status IN ('pending','running')",
            (run_id,),
        )
        row = cur.fetchone()
        return int(row["c"]) if row else 0

