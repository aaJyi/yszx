@echo off
chcp 65001 >nul
cd /d "%~dp0.."
set PYTHON_EXE=%~dp0..\venv\Scripts\python.exe
if not exist "%PYTHON_EXE%" set PYTHON_EXE=python
echo Starting TensorBoard...
echo Open browser: http://localhost:6006
echo Press Ctrl+C to exit
echo.
"%PYTHON_EXE%" -m tensorboard.main --logdir runs --port 6006
pause
