@echo off
cd /d "%~dp0"
python -m uvicorn social.http_api:app --host 127.0.0.1 --port 8095
