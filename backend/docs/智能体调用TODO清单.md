# 智能体调用 TODO 清单

供智能体开发程序员后续更新使用。所有涉及智能体调用的位置均已开启并添加 TODO 注释。

## 一、配置

| 文件 | 说明 |
|------|------|
| `src/main/resources/application.properties` | `ai.agent.base.url`、`medical.ai.base.url`：后续更新为实际智能体服务地址 |

## 二、后端智能体调用点

### 1. 对话类（ConsultationServiceImpl）

| 位置 | TODO 说明 |
|------|-----------|
| `ConsultationServiceImpl.chat()` | 更新 ai.agent.base.url、determineChatUrl、请求/响应格式 |
| `ConsultationServiceImpl.chatStream()` | 更新流式接口 URL、请求/响应格式 |

**对应前端入口**：首页对话、在线问诊(consultation)、AI医生(ai-consultation)

### 2. 心理咨询（PsychologicalConsultationController）

| 位置 | TODO 说明 |
|------|-----------|
| `PsychologicalConsultationController` | 可增加心理咨询专属 prompt 或上下文 |

**对应前端入口**：心理咨询(psychological-consultation)

### 3. 健康档案生成（HealthArchiveProcessServiceImpl）

| 位置 | TODO 说明 |
|------|-----------|
| `callAiAgentToGenerateRemainingData()` | 已对接 medical-ai `/webhook/archive-created` |

**调用时机**：用户执行「生成最新健康档案」后，异步通知智能体生成剩余数据（生活方式、心理评估、建议等）

**智能体 medical-ai 实现**：`main.py` 中 `POST /webhook/archive-created`，`archive_created_handler.py` 实现拉取数据、生成分析建议、提交

### 4. 原始健康数据（RawHealthDataServiceImpl）

| 位置 | TODO 说明 |
|------|-----------|
| `notifyMedicalAiDataChanged()` | 更新 medical.ai.base.url、rebuild 接口地址、请求/响应格式 |

**调用时机**：用户上传报告、就诊记录、检验数据等原始健康数据后，异步通知智能体触发重建

### 5. AI 健康分析（HealthAiAnalysisServiceImpl）

| 位置 | TODO 说明 |
|------|-----------|
| `saveAnalysisAndGenerateRecommendations()` | 更新智能体建议接口、extendedData 格式 |

**说明**：接收智能体提交的分析结果与建议，优先保存智能体建议，失败时使用规则引擎

## 三、Controller 入口（已对接智能体）

| Controller | 接口 | 前端调用页面 |
|------------|------|--------------|
| HomeChatController | POST /home-chat/ask | 首页对话 |
| ConsultationController | POST /consultation/chat、/consultation/chat/stream | 在线问诊、AI医生 |
| PsychologicalConsultationController | POST /psychological-chat/chat、/psychological-chat/chat/stream | 心理咨询 |

## 四、智能体需实现的接口（供参考）

根据后端调用，智能体需提供：

1. **对话**：`POST {ai.agent.base.url}/chat`  
   - 请求：`{userId, question, history?}`  
   - 响应：`{answer}` 或 `{data: {answer}}`

2. **流式对话**：`POST {ai.agent.base.url}/chat/stream`  
   - 请求同上，响应：SSE 流

3. **健康档案创建通知**：`POST {medical.ai.base.url}/webhook/archive-created`  
   - 请求：`{userId, archiveId, event, timestamp}`

4. **数据变更重建**：`POST {medical.ai.base.url}/health-archive-process/rebuild`  
   - 请求：`{userId, event, updateTime}`

5. **健康分析提交**：后端提供 `/health-ai-analysis/submit/user/{userId}`，智能体分析后调用此接口提交结果
