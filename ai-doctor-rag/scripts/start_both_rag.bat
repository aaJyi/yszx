@echo off
REM 同时启动中文 RAG（8765）与英文 RAG（8766）。请在 ai-doctor-rag 目录已配置好 Python 依赖与 Ollama。
cd /d "%~dp0\.."

start "ai-doctor-rag 中文" cmd /k "set AI_DOCTOR_RAG_INDEX_SOURCE=chinese_bge&& set AI_DOCTOR_RAG_PORT=8765&& python app.py"
timeout /t 2 /nobreak >nul
start "ai-doctor-rag 英文" cmd /k "set AI_DOCTOR_RAG_INDEX_SOURCE=english_bge&& set AI_DOCTOR_RAG_PORT=8766&& python app.py"

echo 已尝试打开两个窗口：中文 :8765  英文 :8766
echo 首次使用请先分别构建向量库：python build_index_ch_chroma.py  与  python build_index_en_chroma.py
