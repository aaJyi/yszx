#!/usr/bin/env python3
"""
按 organ_top3_titles.json 中每个器官的 3 个英文维基标题，批量拉取 intro 摘要。
去重后请求，减少 API 次数；输出按器官分组的 JSON。

执行示例（在 organ_disease_crawl 目录下）：
  python fetch_organs_top3.py
  python fetch_organs_top3.py --out extracts_by_organ.json --delay 1.5
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from typing import Any, Dict, List, Tuple

import requests

API = "https://en.wikipedia.org/w/api.php"
HEADERS = {
    "User-Agent": "HappyLifeOrganKnowledgeBot/1.0 (https://example.local; educational)",
}


def load_config(path: str) -> Dict[str, Any]:
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def fetch_extracts_by_titles(titles: List[str], delay_sec: float) -> Dict[str, str]:
    """返回 canonical title -> extract（缺失的标题不在 dict 中）。"""
    result: Dict[str, str] = {}
    batch_size = 8
    for i in range(0, len(titles), batch_size):
        batch = titles[i : i + batch_size]
        params = {
            "action": "query",
            "format": "json",
            "prop": "extracts",
            "explaintext": 1,
            "exintro": 1,
            "titles": "|".join(batch),
        }
        r = requests.get(API, params=params, headers=HEADERS, timeout=45)
        r.raise_for_status()
        data = r.json()
        pages = data.get("query", {}).get("pages", {})
        for _pid, page in pages.items():
            if "missing" in page:
                continue
            title = page.get("title", "")
            extract = (page.get("extract") or "").strip()
            if title:
                result[title] = extract
        time.sleep(delay_sec)
    return result


def resolve_extract(requested: str, title_to_extract: Dict[str, str]) -> Tuple[str, str]:
    """返回 (维基规范标题, 摘要)。找不到则摘要为空。"""
    if requested in title_to_extract:
        return requested, title_to_extract[requested]
    rl = requested.lower()
    for t, ex in title_to_extract.items():
        if t.lower() == rl:
            return t, ex
    return requested, ""


def main() -> int:
    here = os.path.dirname(os.path.abspath(__file__))
    parser = argparse.ArgumentParser(description="按器官拉取各 3 个疾病维基摘要")
    parser.add_argument(
        "--config",
        default=os.path.join(here, "organ_top3_titles.json"),
        help="器官与英文标题映射 JSON",
    )
    parser.add_argument("--out", default="extracts_by_organ.json", help="输出 JSON 路径")
    parser.add_argument("--delay", type=float, default=1.0, help="每批请求间隔（秒）")
    args = parser.parse_args()

    raw = load_config(args.config)
    organ_entries: List[Tuple[str, str, List[str]]] = []
    all_titles_ordered: List[str] = []
    seen = set()

    for key, val in raw.items():
        if key.startswith("_"):
            continue
        if not isinstance(val, dict):
            continue
        zh = val.get("organ_zh", "")
        titles = val.get("titles") or []
        organ_entries.append((key, zh, titles))
        for t in titles:
            t = str(t).strip()
            if not t or t in seen:
                continue
            seen.add(t)
            all_titles_ordered.append(t)

    if not all_titles_ordered:
        print("No titles found in config.", file=sys.stderr)
        return 1

    print(f"Requesting {len(all_titles_ordered)} unique Wikipedia titles in batches...")
    title_to_extract = fetch_extracts_by_titles(all_titles_ordered, args.delay)

    out_root: Dict[str, Any] = {}
    for organ_key, organ_zh, titles in organ_entries:
        diseases = []
        for req_title in titles:
            canon, extract = resolve_extract(req_title.strip(), title_to_extract)
            diseases.append(
                {
                    "requested_title": req_title,
                    "wikipedia_title": canon,
                    "extract": extract,
                    "missing": not bool(extract),
                }
            )
        out_root[organ_key] = {
            "organ_key": organ_key,
            "organ_zh": organ_zh,
            "diseases": diseases,
        }

    out_path = args.out if os.path.isabs(args.out) else os.path.join(here, args.out)
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(out_root, f, ensure_ascii=False, indent=2)

    total_d = sum(len(v["diseases"]) for v in out_root.values())
    missing = sum(1 for v in out_root.values() for d in v["diseases"] if d.get("missing"))
    print(f"Organs: {len(out_root)}, disease slots: {total_d}, missing extracts: {missing}")
    print(f"Wrote -> {out_path}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
