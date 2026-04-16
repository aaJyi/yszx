#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
统一后端服务控制台（Python 微服务）：
- 启停服务
- 查看状态/健康检查
- 查看最近日志
- Web 可视化面板

启动：
  python tools/service_hub.py --port 8787
"""

from __future__ import annotations

import argparse
import atexit
import json
import os
import signal
import subprocess
import sys
import threading
import time
import urllib.request
from pathlib import Path
from typing import Dict, List, Optional

from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse, JSONResponse
import uvicorn


ROOT = Path(__file__).resolve().parent.parent
LOG_DIR = ROOT / "logs" / "python-backends"
LOG_DIR.mkdir(parents=True, exist_ok=True)


def _health_ok(url: str, timeout: float = 1.5) -> bool:
    try:
        req = urllib.request.Request(url, method="GET")
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return 200 <= resp.status < 300
    except Exception:
        return False


def _tail_lines(path: Path, lines: int = 200) -> List[str]:
    if not path.exists():
        return []
    try:
        text = path.read_text(encoding="utf-8", errors="replace")
    except Exception:
        return []
    arr = text.splitlines()
    return arr[-max(1, lines) :]


class ServiceManager:
    def __init__(self, python_exec: str):
        self.python_exec = python_exec
        self._lock = threading.Lock()
        self.procs: Dict[str, subprocess.Popen] = {}

        self.services = {
            "ai-agent": {
                "name": "ai-agent 健康档案",
                "cwd": ROOT / "ai-agent",
                "cmd": [python_exec, "main.py"],
                "health": "http://127.0.0.1:8000/health",
                "log": LOG_DIR / "ai-agent.log",
                "port": 8000,
            },
            "ai-doctor-rag": {
                "name": "ai-doctor-rag 医生问答（中文）",
                "cwd": ROOT / "ai-doctor-rag",
                "cmd": [python_exec, "app.py"],
                "health": "http://127.0.0.1:8765/health",
                "log": LOG_DIR / "ai-doctor-rag.log",
                "port": 8765,
                "env": {
                    "AI_DOCTOR_RAG_INDEX_SOURCE": "chinese_bge",
                    "AI_DOCTOR_RAG_PORT": "8765",
                },
            },
            "ai-doctor-rag-en": {
                "name": "ai-doctor-rag 医生问答（英文 RAG）",
                "cwd": ROOT / "ai-doctor-rag",
                "cmd": [python_exec, "app.py"],
                "health": "http://127.0.0.1:8766/health",
                "log": LOG_DIR / "ai-doctor-rag-en.log",
                "port": 8766,
                "env": {
                    "AI_DOCTOR_RAG_INDEX_SOURCE": "english_bge",
                    "AI_DOCTOR_RAG_PORT": "8766",
                },
            },
            "ai-exercise-train": {
                "name": "ai-exercise-train 运动识别",
                "cwd": ROOT / "ai-exercise-train",
                "cmd": [python_exec, "run_server.py", "--port", "5000"],
                "health": "http://127.0.0.1:5000/docs",
                "log": LOG_DIR / "ai-exercise-train.log",
                "port": 5000,
            },
            "food-train": {
                "name": "food-train 热量识别",
                "cwd": ROOT / "food-train",
                "cmd": [python_exec, "run_server.py", "--port", "5001"],
                "health": "http://127.0.0.1:5001/health",
                "log": LOG_DIR / "food-train.log",
                "port": 5001,
            },
            "crawler-social": {
                "name": "自动化爬虫 social",
                "cwd": ROOT / "crawler-social",
                "cmd": [
                    python_exec,
                    "-m",
                    "uvicorn",
                    "social.http_api:app",
                    "--host",
                    "127.0.0.1",
                    "--port",
                    "8095",
                ],
                "health": "http://127.0.0.1:8095/health",
                "log": LOG_DIR / "crawler-social.log",
                "port": 8095,
            },
            "club-division": {
                "name": "club-division 社团划分",
                "cwd": ROOT / "club-division",
                "cmd": [python_exec, "run_server.py", "--port", "8600"],
                "health": "http://127.0.0.1:8600/health",
                "log": LOG_DIR / "club-division.log",
                "port": 8600,
            },
        }

    def _popen_flags(self):
        flags = 0
        if os.name == "nt":
            flags = subprocess.CREATE_NEW_PROCESS_GROUP  # type: ignore[attr-defined]
        return flags

    def start(self, sid: str) -> Dict:
        with self._lock:
            if sid not in self.services:
                raise KeyError(sid)
            proc = self.procs.get(sid)
            if proc and proc.poll() is None:
                return {"ok": True, "message": "already running"}

            cfg = self.services[sid]
            cfg["log"].parent.mkdir(parents=True, exist_ok=True)
            f = open(cfg["log"], "a", encoding="utf-8", buffering=1)
            f.write(f"\n===== START {time.strftime('%Y-%m-%d %H:%M:%S')} =====\n")
            env = os.environ.copy()
            extra_env = cfg.get("env")
            if extra_env:
                env.update(extra_env)
            p = subprocess.Popen(
                cfg["cmd"],
                cwd=str(cfg["cwd"]),
                stdout=f,
                stderr=subprocess.STDOUT,
                creationflags=self._popen_flags(),
                env=env,
            )
            self.procs[sid] = p
            return {"ok": True, "pid": p.pid}

    def stop(self, sid: str) -> Dict:
        with self._lock:
            p = self.procs.get(sid)
            if not p or p.poll() is not None:
                return {"ok": True, "message": "not running"}
            try:
                if os.name == "nt":
                    p.send_signal(signal.CTRL_BREAK_EVENT)  # type: ignore[attr-defined]
                    time.sleep(1.0)
                p.terminate()
                p.wait(timeout=8)
            except Exception:
                p.kill()
            return {"ok": True}

    def start_all(self) -> Dict:
        out = {}
        for sid in self.services:
            out[sid] = self.start(sid)
        return out

    def stop_all(self) -> Dict:
        out = {}
        for sid in self.services:
            out[sid] = self.stop(sid)
        return out

    def status(self) -> List[Dict]:
        rows = []
        for sid, cfg in self.services.items():
            p = self.procs.get(sid)
            running = bool(p and p.poll() is None)
            rows.append(
                {
                    "id": sid,
                    "name": cfg["name"],
                    "running": running,
                    "pid": p.pid if running else None,
                    "port": cfg["port"],
                    "healthUrl": cfg["health"],
                    "healthy": _health_ok(cfg["health"]),
                    "logFile": str(cfg["log"]),
                }
            )
        return rows

    def logs(self, sid: str, lines: int = 200) -> List[str]:
        if sid not in self.services:
            raise KeyError(sid)
        return _tail_lines(self.services[sid]["log"], lines)


def build_app(mgr: ServiceManager) -> FastAPI:
    app = FastAPI(title="Python Service Hub", version="1.0.0")
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    @app.get("/health")
    def health():
        return {"status": "ok", "service": "service-hub"}

    @app.get("/api/services")
    def api_services():
        return mgr.status()

    @app.post("/api/start/{sid}")
    def api_start(sid: str):
        try:
            return mgr.start(sid)
        except KeyError:
            raise HTTPException(status_code=404, detail="service not found")

    @app.post("/api/stop/{sid}")
    def api_stop(sid: str):
        try:
            return mgr.stop(sid)
        except KeyError:
            raise HTTPException(status_code=404, detail="service not found")

    @app.post("/api/start-all")
    def api_start_all():
        return mgr.start_all()

    @app.post("/api/stop-all")
    def api_stop_all():
        return mgr.stop_all()

    @app.get("/api/logs/{sid}")
    def api_logs(sid: str, lines: int = Query(default=150, ge=20, le=2000)):
        try:
            return {"service": sid, "lines": mgr.logs(sid, lines)}
        except KeyError:
            raise HTTPException(status_code=404, detail="service not found")

    @app.get("/", response_class=HTMLResponse)
    def index():
        return HTMLResponse(
            """
<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1"/>
  <title>Python 后端服务控制台</title>
  <style>
    body{font-family:Arial,Helvetica,sans-serif;background:#0f172a;color:#e2e8f0;margin:0;padding:16px}
    .bar{display:flex;gap:8px;flex-wrap:wrap;margin-bottom:12px}
    button{padding:8px 12px;border:0;border-radius:8px;background:#1d4ed8;color:#fff;cursor:pointer}
    button.danger{background:#dc2626}
    .grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(290px,1fr));gap:12px}
    .card{background:#111827;border:1px solid #334155;border-radius:10px;padding:12px}
    .ok{color:#22c55e}.bad{color:#f59e0b}.off{color:#94a3b8}
    pre{background:#020617;border:1px solid #334155;border-radius:10px;padding:10px;max-height:360px;overflow:auto;white-space:pre-wrap}
    .small{font-size:12px;color:#94a3b8}
  </style>
</head>
<body>
  <h2>后端服务可视化控制台</h2>
  <div class="small">流程：统一虚拟环境 (.venv-all) → 启动各服务 → 健康检查 → 查看日志</div>
  <div class="bar">
    <button onclick="callApi('/api/start-all')">启动全部</button>
    <button class="danger" onclick="callApi('/api/stop-all')">停止全部</button>
    <button onclick="refreshAll()">刷新状态</button>
  </div>
  <div id="cards" class="grid"></div>
  <h3>日志</h3>
  <div class="small">点击任意服务卡片中的“查看日志”</div>
  <pre id="logbox"></pre>
<script>
let currentLogService = "";
async function callApi(url, method="POST"){
  await fetch(url,{method});
  await refreshAll();
}
function badge(r){
  if(!r.running) return '<span class="off">未运行</span>';
  return r.healthy ? '<span class="ok">运行中/健康</span>' : '<span class="bad">运行中/未通过健康检查</span>';
}
async function showLogs(sid){
  currentLogService = sid;
  const res = await fetch(`/api/logs/${sid}?lines=300`);
  const data = await res.json();
  document.getElementById('logbox').textContent = (data.lines||[]).join("\\n");
}
async function refreshAll(){
  const res = await fetch('/api/services');
  const data = await res.json();
  const html = data.map(r => `
    <div class="card">
      <h4>${r.name}</h4>
      <div>${badge(r)}</div>
      <div class="small">id=${r.id} | pid=${r.pid??'-'} | port=${r.port}</div>
      <div class="small"><a href="${r.healthUrl}" target="_blank" style="color:#93c5fd">${r.healthUrl}</a></div>
      <div class="bar" style="margin-top:8px">
        <button onclick="callApi('/api/start/${r.id}')">启动</button>
        <button class="danger" onclick="callApi('/api/stop/${r.id}')">停止</button>
        <button onclick="showLogs('${r.id}')">查看日志</button>
      </div>
    </div>
  `).join("");
  document.getElementById('cards').innerHTML = html;
  if(currentLogService){ showLogs(currentLogService); }
}
refreshAll();
setInterval(refreshAll, 5000);
setInterval(()=>{ if(currentLogService) showLogs(currentLogService); }, 2000);
</script>
</body>
</html>
            """
        )

    return app


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=8787)
    args = parser.parse_args()

    mgr = ServiceManager(sys.executable)
    app = build_app(mgr)
    atexit.register(mgr.stop_all)
    uvicorn.run(app, host="0.0.0.0", port=args.port)


if __name__ == "__main__":
    main()

