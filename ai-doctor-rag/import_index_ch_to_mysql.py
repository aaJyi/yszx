#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
将 D:/happyLife/ai-doctor-rag/data/index_ch 的中文数据写入 MySQL。

目标表（与当前库结构对应）：
- rag_kb_qa            <- qa.json
- rag_kb_liver_cancer  <- liver_cancer.json
- rag_kb_llama         <- llama_data.json
- rag_kb_drug          <- drug.json

用法示例：
python import_index_ch_to_mysql.py ^
  --host 127.0.0.1 --port 3306 --user root --password 123456 --database happy_life ^
  --truncate
"""

import argparse
import json
import os
import re
from typing import Dict, Iterable, List, Tuple

import pymysql


def _strip_html(text: str) -> str:
    if not text:
        return ""
    return re.sub(r"<[^>]+>", "", str(text)).strip()


def _read_json_or_jsonl(path: str) -> List[Dict]:
    with open(path, "r", encoding="utf-8") as f:
        raw = f.read().strip()
    if not raw:
        return []
    if raw.startswith("["):
        data = json.loads(raw)
        if isinstance(data, list):
            return data
        return [data]
    return [json.loads(line) for line in raw.splitlines() if line.strip()]


def _qa_rows(path: str) -> List[Tuple[str, str]]:
    out: List[Tuple[str, str]] = []
    for item in _read_json_or_jsonl(path):
        instruction = (item.get("instruction") or item.get("input") or "").strip()
        output = (item.get("output") or "").strip()
        if not instruction and not output:
            continue
        out.append((instruction, output))
    return out


def _drug_rows(path: str) -> List[Tuple[str, str, str]]:
    out: List[Tuple[str, str, str]] = []
    data = _read_json_or_jsonl(path)
    for item in data:
        drug_name = str(item.get("药品", "")).strip()
        indication_text = _strip_html(item.get("药品适应症", ""))
        diseases_labeled = str(item.get("医学专家标注的适应病症", "")).strip()
        if not drug_name and not indication_text and not diseases_labeled:
            continue
        out.append((drug_name, indication_text, diseases_labeled))
    return out


def _batch_insert(
    conn,
    sql: str,
    rows: Iterable[Tuple],
    batch_size: int = 1000,
) -> int:
    rows = list(rows)
    total = len(rows)
    if total == 0:
        return 0
    inserted = 0
    with conn.cursor() as cur:
        for i in range(0, total, batch_size):
            chunk = rows[i : i + batch_size]
            cur.executemany(sql, chunk)
            inserted += len(chunk)
    return inserted


def import_all(
    host: str,
    port: int,
    user: str,
    password: str,
    database: str,
    base_dir: str,
    truncate: bool,
) -> None:
    qa_path = os.path.join(base_dir, "qa.json")
    liver_path = os.path.join(base_dir, "liver_cancer.json")
    llama_path = os.path.join(base_dir, "llama_data.json")
    drug_path = os.path.join(base_dir, "drug.json")

    if not os.path.isdir(base_dir):
        raise FileNotFoundError(f"index_ch 目录不存在: {base_dir}")

    conn = pymysql.connect(
        host=host,
        port=port,
        user=user,
        password=password,
        database=database,
        charset="utf8mb4",
        autocommit=False,
    )
    try:
        with conn.cursor() as cur:
            if truncate:
                cur.execute("TRUNCATE TABLE rag_kb_qa")
                cur.execute("TRUNCATE TABLE rag_kb_liver_cancer")
                cur.execute("TRUNCATE TABLE rag_kb_llama")
                cur.execute("TRUNCATE TABLE rag_kb_drug")

        qa_rows = _qa_rows(qa_path) if os.path.isfile(qa_path) else []
        liver_rows = _qa_rows(liver_path) if os.path.isfile(liver_path) else []
        llama_rows = _qa_rows(llama_path) if os.path.isfile(llama_path) else []
        drug_rows = _drug_rows(drug_path) if os.path.isfile(drug_path) else []

        n1 = _batch_insert(
            conn,
            "INSERT INTO rag_kb_qa (instruction, output) VALUES (%s, %s)",
            qa_rows,
        )
        n2 = _batch_insert(
            conn,
            "INSERT INTO rag_kb_liver_cancer (instruction, output) VALUES (%s, %s)",
            liver_rows,
        )
        n3 = _batch_insert(
            conn,
            "INSERT INTO rag_kb_llama (instruction, output) VALUES (%s, %s)",
            llama_rows,
        )
        n4 = _batch_insert(
            conn,
            "INSERT INTO rag_kb_drug (drug_name, indication_text, diseases_labeled) VALUES (%s, %s, %s)",
            drug_rows,
        )

        conn.commit()
        print("导入完成：")
        print(f"- rag_kb_qa: {n1}")
        print(f"- rag_kb_liver_cancer: {n2}")
        print(f"- rag_kb_llama: {n3}")
        print(f"- rag_kb_drug: {n4}")
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


def main():
    parser = argparse.ArgumentParser(description="将 index_ch 数据导入 MySQL")
    parser.add_argument("--host", default=os.getenv("MYSQL_HOST", "127.0.0.1"))
    parser.add_argument("--port", type=int, default=int(os.getenv("MYSQL_PORT", "3306")))
    parser.add_argument("--user", default=os.getenv("MYSQL_USER", "root"))
    parser.add_argument("--password", default=os.getenv("MYSQL_PASSWORD", ""))
    parser.add_argument("--database", default=os.getenv("MYSQL_DB", "happy_life"))
    parser.add_argument(
        "--base-dir",
        default=os.getenv("INDEX_CH_DIR", "D:/happyLife/ai-doctor-rag/data/index_ch"),
        help="index_ch 数据目录",
    )
    parser.add_argument("--truncate", action="store_true", help="导入前清空目标表")
    args = parser.parse_args()

    import_all(
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password,
        database=args.database,
        base_dir=args.base_dir,
        truncate=args.truncate,
    )


if __name__ == "__main__":
    main()

