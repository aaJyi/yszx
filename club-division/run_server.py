#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
club-division HTTP 服务（封装 run_ncss_json.py）

启动:
  python run_server.py --port 8600
"""

import json
import subprocess
import sys
from pathlib import Path

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import uvicorn


BASE_DIR = Path(__file__).resolve().parent
SCRIPT = BASE_DIR / "run_ncss_json.py"

app = FastAPI(title="club-division API", version="1.0.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
def health():
    return {"status": "ok", "service": "club-division"}


@app.post("/divide")
def divide(payload: dict):
    if not SCRIPT.is_file():
        raise HTTPException(status_code=500, detail=f"找不到脚本: {SCRIPT}")
    try:
        p = subprocess.run(
            [sys.executable, str(SCRIPT)],
            input=json.dumps(payload, ensure_ascii=False),
            text=True,
            capture_output=True,
            cwd=str(BASE_DIR),
            timeout=600,
            check=False,
        )
        out = (p.stdout or "").strip()
        if p.returncode != 0:
            raise HTTPException(status_code=500, detail=f"run_ncss_json 失败: {p.stderr[-1000:]}")
        if not out:
            raise HTTPException(status_code=500, detail="run_ncss_json 无输出")
        return json.loads(out)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


def main():
    import argparse

    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=8600)
    args = parser.parse_args()
    uvicorn.run(app, host="0.0.0.0", port=args.port)


if __name__ == "__main__":
    main()

