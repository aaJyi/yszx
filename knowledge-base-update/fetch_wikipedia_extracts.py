#!/usr/bin/env python3

from __future__ import annotations

import argparse
import json
import sys
import time
from typing import List

import requests

API = "https://en.wikipedia.org/w/api.php"
HEADERS = {
    "User-Agent": "HappyLifeOrganKnowledgeBot/1.0 (https://example.local; educational)",
}


def fetch_extracts(titles: List[str], delay_sec: float = 1.0) -> List[dict]:
    out: list[dict] = []
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
        r = requests.get(API, params=params, headers=HEADERS, timeout=30)
        r.raise_for_status()
        data = r.json()
        pages = data.get("query", {}).get("pages", {})
        for _pid, page in pages.items():
            if "missing" in page:
                continue
            title = page.get("title", "")
            extract = page.get("extract", "") or ""
            out.append({"title": title, "extract": extract.strip()})
        time.sleep(delay_sec)
    return out


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--titles", required=True, help="english titles separated by comma")
    parser.add_argument("--out", default="extracts.json")
    parser.add_argument("--delay", type=float, default=1.0)
    args = parser.parse_args()
    titles = [t.strip() for t in args.titles.split(",") if t.strip()]
    rows = fetch_extracts(titles, delay_sec=args.delay)
    with open(args.out, "w", encoding="utf-8") as f:
        json.dump(rows, f, ensure_ascii=False, indent=2)
    print(f"Wrote {len(rows)} entries -> {args.out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())

