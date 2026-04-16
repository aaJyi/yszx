# 服务独立虚拟环境启动说明（Windows / CMD）

目标：每个包使用自己的虚拟环境；不再使用 `.venv-all`。

---

## 1. 环境命名约定（按功能名）

- `ai-agent`：`.venv-ai-agent`
- `ai-doctor-rag` 中文：`.venv-rag-cn`
- `ai-doctor-rag` 英文：`.venv-rag-en`
- `ai-exercise-train`：`.venv-exercise`
- `club-division`：`.venv-club-division`
- `crawler-social`：`.venv-crawler-social`
- `food-train`：`.venv-food-train`

---

## 2. 首次创建虚拟环境（每个包只做一次）

> 约定：每次切换环境前先执行 `deactivate`（若当前未激活虚拟环境可忽略报错）。

### 2.1 ai-agent

```bat
deactivate
cd D:\happyLife\ai-agent
python -m venv .venv-ai-agent
.\.venv-ai-agent\Scripts\activate.bat
python -m pip install -U pip
pip install -r requirements.txt
```

### 2.2 ai-doctor-rag（中文环境）

```bat
deactivate
cd D:\happyLife\ai-doctor-rag
python -m venv .venv-rag-cn
.\.venv-rag-cn\Scripts\activate.bat
python -m pip install -U pip
pip install -r requirements.txt
```

### 2.3 ai-doctor-rag（英文环境）

```bat
deactivate
cd D:\happyLife\ai-doctor-rag
python -m venv .venv-rag-en
.\.venv-rag-en\Scripts\activate.bat
python -m pip install -U pip
pip install -r requirements.txt
```

### 2.4 ai-exercise-train

```bat
deactivate
cd D:\happyLife\ai-exercise-train
python -m venv .venv-exercise
.\.venv-exercise\Scripts\activate.bat
python -m pip install -U pip
pip install -r requirements.txt
```

### 2.5 club-division

```bat
deactivate
cd D:\happyLife\club-division
python -m venv .venv-club-division
.\.venv-club-division\Scripts\activate.bat
python -m pip install -U pip
pip install -r requirements.txt
```

### 2.6 crawler-social

```bat
deactivate
cd D:\happyLife\crawler-social
python -m venv .venv-crawler-social
.\.venv-crawler-social\Scripts\activate.bat
python -m pip install -U pip
pip install -r requirements.txt
```

### 2.7 food-train

```bat
deactivate
cd D:\happyLife\food-train
python -m venv .venv-food-train
.\.venv-food-train\Scripts\activate.bat
python -m pip install -U pip
pip install -r requirements.txt
```

---

## 3. 后续直接启动服务（每次开服务执行）

> 每个服务用独立终端启动，互不影响。

### 3.1 ai-agent（8000，可选）

```bat
deactivate
cd D:\happyLife\ai-agent
.\.venv-ai-agent\Scripts\activate.bat
python main.py
```

### 3.2 ai-doctor-rag 中文（8765）

```bat
deactivate
cd D:\happyLife\ai-doctor-rag
.\.venv-rag-cn\Scripts\activate.bat
set AI_DOCTOR_RAG_INDEX_SOURCE=chinese_bge
set AI_DOCTOR_RAG_PORT=8765
python app.py
```

### 3.3 ai-doctor-rag 英文（8766）

```bat
deactivate
cd D:\happyLife\ai-doctor-rag
.\.venv-rag-en\Scripts\activate.bat
set AI_DOCTOR_RAG_INDEX_SOURCE=english_bge
set AI_DOCTOR_RAG_PORT=8766
python app.py
```

### 3.4 ai-exercise-train（5000）

```bat
deactivate
cd D:\happyLife\ai-exercise-train
.\.venv-exercise\Scripts\activate.bat
python run_server.py --port 5000
```

### 3.5 club-division（8600）

```bat
deactivate
cd D:\happyLife\club-division
.\.venv-club-division\Scripts\activate.bat
python run_server.py --port 8600
```

### 3.6 crawler-social（8095）

```bat
deactivate
cd D:\happyLife\crawler-social
.\.venv-crawler-social\Scripts\activate.bat
python -m uvicorn social.http_api:app --host 127.0.0.1 --port 8095
```

### 3.7 food-train（5001）

```bat
deactivate
cd D:\happyLife\food-train
.\.venv-food-train\Scripts\activate.bat
python run_server.py --port 5001
```

### 3.8 backend（Java，8080）

```bat
cd D:\happyLife\backend
mvn spring-boot:run
```

### 3.9 真机调试

```bat
New-NetFirewallRule -DisplayName "HappyLife Backend 8080" -Direction Inbound -LocalPort 8080 -Protocol TCP -Action Allow
```


---

## 4. 常用健康检查地址

- `http://127.0.0.1:8765/health`（中文 RAG）
- `http://127.0.0.1:8766/health`（英文 RAG）
- `http://127.0.0.1:5000/docs`（运动识别）
- `http://127.0.0.1:5001/health`（食物识别）
- `http://127.0.0.1:8095/health`（crawler-social）
- `http://127.0.0.1:8600/health`（club-division）
- `http://127.0.0.1:8000/health`（ai-agent，如实现了 health）
- `http://127.0.0.1:8080`（SpringBoot）

---

## 5. `.venv-all` 是否可删除

可以。等以上独立环境全部可用后，即可删除：

```bat
rmdir /s /q D:\happyLife\.venv-all
```

