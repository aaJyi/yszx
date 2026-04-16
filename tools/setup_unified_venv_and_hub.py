#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
纯 Python 初始化脚本：
1) 创建统一虚拟环境 .venv-all
2) 安装六个后端服务依赖
3) 启动可视化服务控制台 service_hub.py

用法：
  python tools/setup_unified_venv_and_hub.py
  python tools/setup_unified_venv_and_hub.py --skip-install
"""

from __future__ import annotations

import argparse
import os
import subprocess
import sys
import venv
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
VENV_DIR = ROOT / ".venv-all"


def run(cmd, cwd=None):
    print("[RUN]", " ".join(map(str, cmd)))
    subprocess.run(cmd, cwd=cwd, check=True)


def venv_python() -> Path:
    if os.name == "nt":
        return VENV_DIR / "Scripts" / "python.exe"
    return VENV_DIR / "bin" / "python"


def ensure_venv():
    py = venv_python()
    if py.exists():
        return
    print("[STEP] 创建虚拟环境:", VENV_DIR)
    venv.EnvBuilder(with_pip=True).create(str(VENV_DIR))


def install_requirements(py: Path):
    reqs = [
        ROOT / "ai-agent" / "requirements.txt",
        ROOT / "ai-doctor-rag" / "requirements.txt",
        ROOT / "ai-exercise-train" / "requirements.txt",
        ROOT / "food-train" / "requirements.txt",
        ROOT / "crawler-social" / "requirements.txt",
        ROOT / "club-division" / "requirements.txt",
        ROOT / "knowledge-base-update" / "requirements.txt",
    ]

    run([str(py), "-m", "pip", "install", "-U", "pip", "wheel", "setuptools"])
    run([str(py), "-m", "pip", "install", "fastapi", "uvicorn"])

    for r in reqs:
        if r.exists():
            run([str(py), "-m", "pip", "install", "-r", str(r)])
        else:
            print("[WARN] requirements 不存在，跳过:", r)


def start_hub(py: Path, port: int):
    print(f"[INFO] 打开控制台: http://127.0.0.1:{port}")
    run([str(py), str(ROOT / "tools" / "service_hub.py"), "--port", str(port)])


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--skip-install", action="store_true", help="跳过安装依赖")
    parser.add_argument("--port", type=int, default=8787, help="service hub 端口")
    args = parser.parse_args()

    ensure_venv()
    py = venv_python()
    if not py.exists():
        raise SystemExit("虚拟环境 Python 不存在，创建失败")

    if not args.skip_install:
        install_requirements(py)

    start_hub(py, args.port)


if __name__ == "__main__":
    main()

