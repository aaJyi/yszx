#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
将 D:/happyLife/ai-doctor-rag/data/index/docs.csv 导入 MySQL 表 rag_kb_en_doc。

目标表：
- rag_kb_en_doc(input, output)

用法示例：
python import_index_docs_csv_to_mysql.py ^
  --host 127.0.0.1 --port 3306 --user root --password 123456 --database happy_life ^
  --truncate
"""

import argparse
import csv
import os
from typing import Iterable, List, Tuple

import pymysql


def read_docs_csv(path: str) -> List[Tuple[str, str]]:
    if not os.path.isfile(path):
        raise FileNotFoundError(f"docs.csv 不存在: {path}")

    rows: List[Tuple[str, str]] = []
    with open(path, "r", encoding="utf-8", newline="") as f:
        reader = csv.DictReader(f)
        for item in reader:
            q = (item.get("input") or "").strip()
            a = (item.get("output") or "").strip()
            if not q and not a:
                continue
            rows.append((q, a))
    return rows


def batch_insert(conn, sql: str, rows: Iterable[Tuple[str, str]], batch_size: int = 2000) -> int:
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


def import_docs(
    host: str,
    port: int,
    user: str,
    password: str,
    database: str,
    csv_path: str,
    truncate: bool,
) -> None:
    rows = read_docs_csv(csv_path)

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
                cur.execute("TRUNCATE TABLE rag_kb_en_doc")

        n = batch_insert(
            conn,
            "INSERT INTO rag_kb_en_doc (input, output) VALUES (%s, %s)",
            rows,
        )
        conn.commit()
        print(f"导入完成: rag_kb_en_doc = {n}")
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


def main():
    parser = argparse.ArgumentParser(description="将 docs.csv 导入 rag_kb_en_doc")
    parser.add_argument("--host", default=os.getenv("MYSQL_HOST", "127.0.0.1"))
    parser.add_argument("--port", type=int, default=int(os.getenv("MYSQL_PORT", "3306")))
    parser.add_argument("--user", default=os.getenv("MYSQL_USER", "root"))
    parser.add_argument("--password", default=os.getenv("MYSQL_PASSWORD", ""))
    parser.add_argument("--database", default=os.getenv("MYSQL_DB", "happy_life"))
    parser.add_argument(
        "--csv-path",
        default=os.getenv("INDEX_DOCS_CSV", "D:/happyLife/ai-doctor-rag/data/index/docs.csv"),
        help="docs.csv 文件路径",
    )
    parser.add_argument("--truncate", action="store_true", help="导入前清空 rag_kb_en_doc")
    args = parser.parse_args()

    import_docs(
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password,
        database=args.database,
        csv_path=args.csv_path,
        truncate=args.truncate,
    )


if __name__ == "__main__":
    main()

