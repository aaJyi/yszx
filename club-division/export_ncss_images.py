#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
从 stdin 读入与 run_ncss_json.py 相同 JSON，使用 ncss_matplotlib.NCSS 生成
img/dashboard.png 与 img/expansion_*.png，向 stdout 输出 {"ok":true,"files":[...]}。
需：numpy matplotlib networkx（与 ncss_matplotlib 一致）。
"""
import contextlib
import json
import os
import sys

os.environ.setdefault("MPLBACKEND", "Agg")


@contextlib.contextmanager
def silence_stdout():
    with open(os.devnull, "w", encoding="utf-8") as devnull:
        old = sys.stdout
        sys.stdout = devnull
        try:
            yield
        finally:
            sys.stdout = old


def main():
    try:
        raw = sys.stdin.read()
        if not raw.strip():
            print(json.dumps({"ok": False, "error": "empty stdin"}, ensure_ascii=False))
            return
        data = json.loads(raw)
        th = float(data.get("similarity_threshold", 0.3))
        ud = data.get("user_diseases") or {}
        user_diseases = {str(k): set(v or []) for k, v in ud.items()}
        user_diseases = {k: v for k, v in user_diseases.items() if v}
        if len(user_diseases) < 2:
            print(json.dumps({"ok": False, "error": "用户数不足"}, ensure_ascii=False))
            return

        from ncss_matplotlib import NCSS

        ncss = NCSS(
            damping_factor=0.8,
            epsilon=1e-6,
            max_iterations=100,
            dependency_threshold=0.5,
            min_community_ratio=0.05,
        )
        ncss.user_diseases = user_diseases

        with silence_stdout():
            ncss.detect_communities(user_diseases, similarity_threshold=th)

        script_dir = os.path.dirname(os.path.abspath(__file__))
        img_dir = os.path.join(script_dir, "img")
        os.makedirs(img_dir, exist_ok=True)

        files = []
        dash = os.path.join(img_dir, "dashboard.png")
        if ncss.export_dashboard_png(dash):
            files.append("dashboard.png")
        files.extend(ncss.export_expansion_frames(img_dir, max_frames=48))

        print(json.dumps({"ok": True, "imgDir": img_dir, "files": files}, ensure_ascii=False))
    except Exception as e:
        print(json.dumps({"ok": False, "error": str(e)}, ensure_ascii=False))


if __name__ == "__main__":
    if sys.platform == "win32":
        try:
            sys.stdout.reconfigure(encoding="utf-8")
        except Exception:
            pass
    main()
