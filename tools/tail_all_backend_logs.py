#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import time
from pathlib import Path


def main():
    root = Path(__file__).resolve().parent.parent
    log_dir = root / "logs" / "python-backends"
    log_dir.mkdir(parents=True, exist_ok=True)

    files = [
        ("ai-agent", log_dir / "ai-agent.log"),
        ("ai-doctor-rag", log_dir / "ai-doctor-rag.log"),
        ("ai-exercise-train", log_dir / "ai-exercise-train.log"),
        ("food-train", log_dir / "food-train.log"),
        ("crawler-social", log_dir / "crawler-social.log"),
        ("club-division", log_dir / "club-division.log"),
    ]

    positions = {}
    for name, fp in files:
        fp.touch(exist_ok=True)
        positions[name] = fp.stat().st_size

    print(f"[INFO] 监听目录: {log_dir}")
    print("[INFO] Ctrl+C 退出")

    try:
        while True:
            for name, fp in files:
                try:
                    size = fp.stat().st_size
                    if size < positions[name]:
                        positions[name] = 0
                    if size > positions[name]:
                        with fp.open("r", encoding="utf-8", errors="replace") as f:
                            f.seek(positions[name])
                            chunk = f.read()
                        positions[name] = size
                        for line in chunk.splitlines():
                            if line.strip():
                                print(f"[{name}] {line}")
                except FileNotFoundError:
                    continue
            time.sleep(0.4)
    except KeyboardInterrupt:
        print("\n[INFO] 停止监听")


if __name__ == "__main__":
    main()

