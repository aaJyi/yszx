#!/usr/bin/env python3
"""Import translated organ disease extracts into MySQL table organ_disease_detail."""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from typing import Any, Dict, List, Tuple

import pymysql
from deep_translator import GoogleTranslator

DATA_SOURCE = "wikipedia_zh"
CHUNK = 4200
SLEEP_BETWEEN_CALLS = 1.0


def getenv(name: str, default: str | None = None) -> str | None:
    v = os.environ.get(name)
    if v is not None and str(v).strip() != "":
        return v.strip()
    return default


def translate_en_to_zh(text: str) -> str:
    if not text or not text.strip():
        return ""
    tr = GoogleTranslator(source="en", target="zh-CN")
    text = text.strip()
    if len(text) <= CHUNK:
        return tr.translate(text)
    parts: List[str] = []
    for i in range(0, len(text), CHUNK):
        chunk = text[i : i + CHUNK]
        parts.append(tr.translate(chunk))
        time.sleep(SLEEP_BETWEEN_CALLS)
    return "".join(parts)


def translate_title(title: str) -> str:
    if not title:
        return ""
    tr = GoogleTranslator(source="en", target="zh-CN")
    return tr.translate(title.strip())


def load_json(path: str) -> Dict[str, Any]:
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def connect_mysql():
    host = getenv("MYSQL_HOST", "localhost")
    port = int(getenv("MYSQL_PORT", "3306") or "3306")
    user = getenv("MYSQL_USER", "root")
    password = getenv("MYSQL_PASSWORD", "sjt")
    database = getenv("MYSQL_DATABASE", getenv("MYSQL_DB", "mvit"))
    if not database:
        raise RuntimeError("Please set MYSQL_DATABASE (or MYSQL_DB)")
    return pymysql.connect(
        host=host,
        port=port,
        user=user,
        password=password,
        database=database,
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
    )


def delete_existing_wikipedia_rows(cur, organ_keys: List[str]) -> int:
    if not organ_keys:
        return 0
    placeholders = ",".join(["%s"] * len(organ_keys))
    sql = f"DELETE FROM organ_disease_detail WHERE data_source = %s AND organ_key IN ({placeholders})"
    cur.execute(sql, [DATA_SOURCE] + organ_keys)
    return cur.rowcount


def insert_row(cur, organ_key: str, organ_zh: str, disease_name: str, description: str, treatment: str, sort_order: int) -> None:
    sql = """
        INSERT INTO organ_disease_detail
        (organ_key, organ_zh, disease_name, description, treatment_measures, data_source, sort_order)
        VALUES (%s, %s, %s, %s, %s, %s, %s)
    """
    cur.execute(sql, (organ_key, organ_zh, disease_name, description, treatment, DATA_SOURCE, sort_order))


def process(data: Dict[str, Any], dry_run: bool, skip_translate: bool) -> List[Tuple[str, str, str, str, str, int]]:
    rows: List[Tuple[str, str, str, str, str, int]] = []
    for okey, block in data.items():
        if okey.startswith("_") or not isinstance(block, dict):
            continue
        organ_zh = (block.get("organ_zh") or "").strip()
        diseases = block.get("diseases") or []
        for idx, d in enumerate(diseases):
            if not isinstance(d, dict) or d.get("missing"):
                continue
            ext = (d.get("extract") or "").strip()
            if not ext:
                continue
            wtitle = (d.get("wikipedia_title") or d.get("requested_title") or "").strip()
            if dry_run:
                name_zh = wtitle
                desc_zh = (ext[:400] + "…") if len(ext) > 400 else ext
                treat_zh = "dry-run"
            elif skip_translate:
                name_zh = wtitle
                desc_zh = ext
                treat_zh = "skip-translate"
            else:
                name_zh = translate_title(wtitle)
                time.sleep(SLEEP_BETWEEN_CALLS)
                desc_zh = translate_en_to_zh(ext)
                time.sleep(SLEEP_BETWEEN_CALLS)
                treat_zh = "以下为疾病概述译文，个体化治疗请遵医嘱。"
            rows.append((okey, organ_zh, name_zh, desc_zh, treat_zh, idx))
    return rows


def main() -> int:
    parser = argparse.ArgumentParser(description="Import extracts_by_organ.json to MySQL")
    parser.add_argument("--json", default="extracts_by_organ.json")
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--skip-translate", action="store_true")
    args = parser.parse_args()

    if not os.path.isfile(args.json):
        print(f"File not found: {args.json}", file=sys.stderr)
        return 1

    raw = load_json(args.json)
    rows = process(raw, dry_run=args.dry_run, skip_translate=args.skip_translate)
    if not rows:
        print("No rows to import.", file=sys.stderr)
        return 1
    if args.dry_run:
        print(f"dry-run rows: {len(rows)}")
        return 0

    conn = connect_mysql()
    try:
        organ_keys = list({r[0] for r in rows})
        with conn.cursor() as cur:
            delete_existing_wikipedia_rows(cur, organ_keys)
            for r in rows:
                insert_row(cur, r[0], r[1], r[2], r[3], r[4], r[5])
        conn.commit()
        print(f"Inserted rows: {len(rows)}")
    finally:
        conn.close()
    return 0


if __name__ == "__main__":
    sys.exit(main())

