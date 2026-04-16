#!/usr/bin/env python3
"""
将 fetch_organs_top3.py 生成的 extracts_by_organ.json 中英摘要译为中文并写入 MySQL 表 organ_disease_detail。

后端已有接口：GET /admin/organ-diseases?organKey=xxx （无需改 Java）

使用前：
  pip install -r requirements.txt

配置数据库（环境变量，或在命令行传入）：
  MYSQL_HOST  MYSQL_PORT  MYSQL_USER  MYSQL_PASSWORD  MYSQL_DATABASE

执行示例：
  set MYSQL_PASSWORD=你的密码
  python import_extracts_to_mysql.py --json extracts_by_organ.json

仅预览不写入：
  python import_extracts_to_mysql.py --json extracts_by_organ.json --dry-run
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from typing import Any, Dict, List, Tuple

try:
    import pymysql
except ImportError:
    pymysql = None  # type: ignore

try:
    from deep_translator import GoogleTranslator
except ImportError:
    GoogleTranslator = None  # type: ignore

DATA_SOURCE = "wikipedia_zh"
CHUNK = 4200
SLEEP_BETWEEN_CALLS = 1.0


def getenv(name: str, default: str | None = None) -> str | None:
    v = os.environ.get(name)
    if v is not None and str(v).strip() != "":
        return v.strip()
    return default


def translate_en_to_zh(text: str) -> str:
    """英文长文分段机翻（需联网）。失败时抛出异常由上层处理。"""
    if not text or not text.strip():
        return ""
    if GoogleTranslator is None:
        raise RuntimeError("请安装: pip install deep-translator")
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
    if GoogleTranslator is None:
        raise RuntimeError("请安装: pip install deep-translator")
    tr = GoogleTranslator(source="en", target="zh-CN")
    return tr.translate(title.strip())


def load_json(path: str) -> Dict[str, Any]:
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def connect_mysql():
    if pymysql is None:
        raise RuntimeError("请安装: pip install pymysql")
    host = getenv("MYSQL_HOST", "localhost")
    port = int(getenv("MYSQL_PORT", "3306") or "3306")
    user = getenv("MYSQL_USER", "root")
    password = getenv("MYSQL_PASSWORD", "sjt")
    database = getenv("MYSQL_DATABASE", getenv("MYSQL_DB", "mvit"))
    if not database:
        raise RuntimeError("请设置 MYSQL_DATABASE（或 MYSQL_DB）")
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
    sql = (
        f"DELETE FROM organ_disease_detail WHERE data_source = %s "
        f"AND organ_key IN ({placeholders})"
    )
    cur.execute(sql, [DATA_SOURCE] + organ_keys)
    return cur.rowcount


def insert_row(
    cur,
    organ_key: str,
    organ_zh: str,
    disease_name: str,
    description: str,
    treatment_measures: str,
    sort_order: int,
) -> None:
    sql = """
        INSERT INTO organ_disease_detail
        (organ_key, organ_zh, disease_name, description, treatment_measures, data_source, sort_order)
        VALUES (%s, %s, %s, %s, %s, %s, %s)
    """
    cur.execute(
        sql,
        (
            organ_key,
            organ_zh,
            disease_name,
            description,
            treatment_measures,
            DATA_SOURCE,
            sort_order,
        ),
    )


def process(
    data: Dict[str, Any],
    dry_run: bool,
    skip_translate: bool,
) -> List[Tuple[str, str, str, str, str, int]]:
    """
    返回待插入行列表：
    (organ_key, organ_zh, disease_name_zh, description_zh, treatment_zh, sort_order)
    """
    rows: List[Tuple[str, str, str, str, str, int]] = []
    for okey, block in data.items():
        if okey.startswith("_"):
            continue
        if not isinstance(block, dict):
            continue
        organ_zh = (block.get("organ_zh") or "").strip()
        diseases = block.get("diseases") or []
        for idx, d in enumerate(diseases):
            if not isinstance(d, dict):
                continue
            if d.get("missing"):
                print(f"[跳过] {okey} 缺失条目: {d.get('requested_title')}", file=sys.stderr)
                continue
            ext = (d.get("extract") or "").strip()
            if not ext:
                continue
            wtitle = (d.get("wikipedia_title") or d.get("requested_title") or "").strip()
            if dry_run:
                name_zh = wtitle
                desc_zh = (ext[:400] + "…") if len(ext) > 400 else ext
                treat_zh = "（dry-run：未机翻、未写库）"
            elif skip_translate:
                name_zh = wtitle
                desc_zh = ext
                treat_zh = "（未翻译，skip-translate 模式）"
            else:
                print(f"[翻译] {okey} ({idx + 1}/3) 疾病标题: {wtitle[:60]}...")
                name_zh = translate_title(wtitle)
                time.sleep(SLEEP_BETWEEN_CALLS)
                print(f"       -> 中文名: {name_zh[:80]}")
                desc_zh = translate_en_to_zh(ext)
                time.sleep(SLEEP_BETWEEN_CALLS)
                treat_zh = (
                    "以下为疾病概述译文，个体化治疗请遵医嘱。"
                    "英文原始条目为教学参考，不替代临床诊疗。"
                )
            rows.append((okey, organ_zh, name_zh, desc_zh, treat_zh, idx))
    return rows


def main() -> int:
    parser = argparse.ArgumentParser(description="将 extracts_by_organ.json 译成中文并导入 MySQL")
    parser.add_argument("--json", default="extracts_by_organ.json", help="输入 JSON 路径")
    parser.add_argument("--dry-run", action="store_true", help="只翻译/打印，不写数据库")
    parser.add_argument(
        "--skip-translate",
        action="store_true",
        help="不调用机翻（英文直接入库，仅调试）",
    )
    args = parser.parse_args()

    if not os.path.isfile(args.json):
        print(f"文件不存在: {args.json}", file=sys.stderr)
        return 1

    raw = load_json(args.json)
    rows = process(raw, dry_run=args.dry_run, skip_translate=args.skip_translate)
    if not rows:
        print("没有可导入的行。", file=sys.stderr)
        return 1

    print(f"共 {len(rows)} 条待写入（data_source={DATA_SOURCE}）")

    if args.dry_run:
        for r in rows[:5]:
            print("--- sample ---")
            print(r)
        print("dry-run：未机翻、未写入数据库")
        return 0

    conn = connect_mysql()
    try:
        organ_keys = list({r[0] for r in rows})
        with conn.cursor() as cur:
            n_del = delete_existing_wikipedia_rows(cur, organ_keys)
            print(f"已删除旧记录（同器官、{DATA_SOURCE}）: {n_del} 行")
            for r in rows:
                insert_row(cur, r[0], r[1], r[2], r[3], r[4], r[5])
        conn.commit()
        print(f"已插入 {len(rows)} 行。")
    finally:
        conn.close()

    print("完成后可直接用后端接口查询，例如: GET /admin/organ-diseases?organKey=brain")
    return 0


if __name__ == "__main__":
    sys.exit(main())
