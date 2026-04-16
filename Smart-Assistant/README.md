# Smart-Assistant（多智能体最小实现 / Python + MySQL + RabbitMQ + Ollama + 联网检索）

这是一个**不接小程序前端**的最小可运行 Demo：用户提交一个大需求后，后端用本地 Ollama 进行任务规划（Planner），把大任务拆成子任务写入 MySQL，并投递到 RabbitMQ；Worker 消费队列并执行各子任务（可联网检索），最后汇总产物供查询与下载。

## 你将得到什么
- **任务分解**：Planner 输出结构化 JSON（数组）
- **多任务执行**：当前版本先用**串行同步执行**跑通逻辑（不依赖消息队列）；后续可再切回 RabbitMQ 异步执行
- **可观测**：MySQL 中可看到 run、task、artifact 的状态流转
- **测试页面**：`test.html` 直接调用后端接口发起任务、轮询结果

## 目录结构
- `docker-compose.yml`：MySQL + RabbitMQ（可选；你也可以用本机已安装的服务）
- `requirements.txt`：Python 依赖
- `app/main.py`：FastAPI（创建 run、生成计划、**串行执行任务**、查询状态）
- `app/worker.py`：RabbitMQ Worker（保留文件，后续要切异步时再启用；当前不需要）
- `app/db.py`：MySQL 访问封装
- `app/llm.py`：Ollama 调用封装
- `app/search.py`：联网检索（DuckDuckGo HTML 结果抓取，零 key）
- `db/init.sql`：初始化表结构
- `test.html`：浏览器测试页
- `.env.example`：环境变量示例

## 前置条件
- Windows 10+（你当前环境 OK）
- **本机 MySQL 已安装并可连接**（推荐 8.0+）
- **本机 RabbitMQ 已安装并可连接**（推荐 3.x）
- 本地已安装并启动 Ollama（默认 `http://127.0.0.1:11434`）
  - 先拉一个模型（示例用 `qwen2.5:7b-instruct`，你也可换）

## 启动方式（无 Docker / 你当前情况）
1) 初始化 MySQL（建库、建表、账号）

```powershell
cd D:\happyLife\Smart-Assistant
```

在 MySQL 客户端里执行 `db/init.sql`（两种任选其一）：

- 方式 A：MySQL 命令行

```powershell
mysql -u root -p < .\db\init.sql
```

- 方式 B：你常用的 GUI（Navicat / DataGrip 等）导入执行

然后创建一个用于应用连接的账号（如果你已经有账号可跳过）：

```sql
CREATE USER IF NOT EXISTS 'sa'@'%' IDENTIFIED BY 'sa_pass';
GRANT ALL PRIVILEGES ON smart_assistant.* TO 'sa'@'%';
FLUSH PRIVILEGES;
```

2) RabbitMQ（当前版本可先不管）
- 本版本不使用消息队列，先跑通串行逻辑即可
- 后续你想恢复异步执行，再启动 RabbitMQ + worker

3) 配置环境变量
- 复制 `.env.example` 为 `.env`，按需修改（尤其是 `OLLAMA_MODEL`）

4) 安装依赖并启动 API

```powershell
python -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000
```

5) 打开测试页
- 直接用浏览器打开 `test.html`
- API 默认地址：`http://127.0.0.1:8000`

## 运行前自检（建议）
- Ollama 是否可用：
  - 浏览器访问：`http://127.0.0.1:11434/`（或用 `ollama list` 看模型是否已拉取）
- MySQL 是否可连：
  - 确认 `.env` 中 `MYSQL_HOST/MYSQL_PORT/MYSQL_USER/MYSQL_PASSWORD`
- RabbitMQ 是否可连：
  - 确认 `.env` 中 `RABBITMQ_HOST/RABBITMQ_PORT/RABBITMQ_USER/RABBITMQ_PASSWORD`

## API（最小）
- `POST /api/runs`
  - body: `{ "prompt": "..." }`
  - 返回：`{ "run_id": "...", "plan": [ ... ] }`
- `GET /api/runs/{run_id}`
  - 返回：run 状态、tasks、artifacts 列表
- `GET /api/runs/{run_id}/artifacts/{artifact_id}`
  - 返回某个产物内容（JSON/text）

## 注意与限制（Demo 的刻意简化）
- Planner/Writer/PPT 都使用同一个 Ollama 模型（你可按角色拆分模型）
- 联网检索采用 DuckDuckGo HTML 抓取，**不保证稳定**（生产建议换成正规 Search API 或自建检索服务）
- PPT 产物 Demo 先输出“逐页 JSON”（不生成 `pptx`）。后续可接入模板渲染生成 `pptx`

## 常见报错排查
### 1) PyMySQL 报 `cryptography` 缺失
如果你看到类似错误：
`RuntimeError: 'cryptography' package is required for sha256_password or caching_sha2_password auth methods`

**务必用「启动 uvicorn 时用的那个 Python」安装**（不要混用 `venv` 与 `.venv`）：

```powershell
cd D:\happyLife\Smart-Assistant
# 任选其一：与你实际用的虚拟环境一致
.\.venv\Scripts\python.exe -m pip install cryptography
# 或
.\venv\Scripts\python.exe -m pip install cryptography
```

也可整包重装依赖：

```powershell
.\.venv\Scripts\activate
pip install -r requirements.txt
```

可选替代方案（不推荐作为默认）：把 MySQL 账号认证方式改成 `mysql_native_password`，例如：

```sql
ALTER USER 'sa'@'%' IDENTIFIED WITH mysql_native_password BY 'sa_pass';
FLUSH PRIVILEGES;
```

