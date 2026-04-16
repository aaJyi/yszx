# 智能体对话接口JSON格式说明

## 一、后端发送给智能体的JSON格式

### 请求URL
```
POST https://3c1d06ab.r30.cpolar.top/chat
```
**注意**：如果智能体接口路径不是 `/chat`，请在 `application.properties` 中配置完整路径，例如：
```
ai.agent.chat.url=https://3c1d06ab.r30.cpolar.top/api/chat
```

### 请求头
```
Content-Type: application/json
```

### 请求体JSON格式

```json
{
  "userId": 2,
  "question": "你好",
  "history": [
    {
      "role": "user",
      "content": "之前的问题"
    },
    {
      "role": "assistant",
      "content": "之前的回答"
    }
  ],
  "userHealthData": {
    "userId": 2,
    "rawHealthDataList": [
      {
        "id": 100,
        "userId": 2,
        "dataType": "REPORT",
        "formatType": "IMAGE",
        "filePath": "/uploads/reports/2025/01/xxx.jpg",
        "fileUrl": "https://example.com/uploads/reports/2025/01/xxx.jpg",
        "rawData": "base64编码的文件内容",
        "createTime": "2025-01-05T10:00:00"
      }
    ],
    "healthArchives": [
      {
        "healthArchive": {
          "archiveId": 1,
          "userId": 2,
          "userName": "张三",
          "archiveNo": "HA20250106001",
          "archiveName": "2025年度体检档案",
          "archiveDate": "2025-01-05",
          "archiveYear": 2025
        },
        "healthProfileTags": {
          "isChild06": false,
          "isElderly65": false,
          "isPregnant": false,
          "bloodType": "A",
          "weightStatus": "超重"
        },
        "userInfo": {
          "fullName": "张三",
          "gender": "男",
          "birthDate": "1980-05-15",
          "personalPhone": "13800138000"
        },
        "allergyHistory": {...},
        "diseaseHistory": [...],
        "familyHistory": [...],
        ...
      }
    ]
  }
}
```

### 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 用户ID |
| question | String | 是 | 用户提问内容 |
| history | List<Object> | 否 | 对话历史，格式为 `[{"role": "user/assistant", "content": "内容"}]` |
| userHealthData | Object | 否 | 用户健康数据（包含原始数据和健康档案） |

### history字段格式

```json
[
  {
    "role": "user",
    "content": "用户的问题1"
  },
  {
    "role": "assistant",
    "content": "AI的回答1"
  },
  {
    "role": "user",
    "content": "用户的问题2"
  }
]
```

**说明**：
- `role`: 角色，`"user"` 表示用户，`"assistant"` 表示AI助手
- `content`: 对话内容

---

## 二、智能体接收并处理的JSON格式

智能体接收到的JSON格式与后端发送的格式**完全一致**。

### 智能体需要处理的字段

1. **userId**：用户ID，可用于查询用户相关信息
2. **question**：当前用户的问题，需要回答的内容
3. **history**：对话历史，可用于上下文理解
4. **userHealthData**：用户健康数据，包含：
   - `rawHealthDataList`：原始健康数据列表（体检报告等）
   - `healthArchives`：健康档案列表（包含所有从表数据）

### 智能体处理建议

1. **使用对话历史**：结合 `history` 字段理解上下文，提供连贯的回答
2. **使用健康数据**：结合 `userHealthData` 提供个性化的健康建议
3. **回答格式**：返回的JSON格式见下方

---

## 三、智能体返回给后端的JSON格式

### 响应状态码
- `200 OK`：成功
- `400 Bad Request`：请求格式错误
- `500 Internal Server Error`：服务器错误

### 成功响应JSON格式（推荐格式1）

```json
{
  "answer": "您好！我是AI健康助手，很高兴为您服务。根据您的健康档案，我注意到您有一些健康指标需要关注..."
}
```

### 成功响应JSON格式（推荐格式2）

```json
{
  "data": {
    "answer": "您好！我是AI健康助手，很高兴为您服务..."
  }
}
```

### 成功响应JSON格式（推荐格式3）

```json
{
  "message": "您好！我是AI健康助手，很高兴为您服务..."
}
```

### 成功响应JSON格式（推荐格式4）

```json
{
  "content": "您好！我是AI健康助手，很高兴为您服务..."
}
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| answer | String | AI回答内容（优先使用） |
| data.answer | String | AI回答内容（嵌套格式） |
| message | String | AI回答内容（备用格式） |
| content | String | AI回答内容（备用格式） |

**注意**：后端会按以下优先级解析回答：
1. `answer` 字段（最优先）
2. `data.answer` 字段
3. `message` 字段
4. `content` 字段

### 错误响应JSON格式

```json
{
  "error": "错误信息",
  "code": 400
}
```

---

## 四、完整示例

### 示例1：简单对话（无历史，无健康数据）

**后端发送**：
```json
{
  "userId": 2,
  "question": "你好"
}
```

**智能体返回**：
```json
{
  "answer": "您好！我是AI健康助手，有什么健康问题可以咨询我。"
}
```

### 示例2：带对话历史的对话

**后端发送**：
```json
{
  "userId": 2,
  "question": "我的血压正常吗？",
  "history": [
    {
      "role": "user",
      "content": "你好"
    },
    {
      "role": "assistant",
      "content": "您好！我是AI健康助手，有什么健康问题可以咨询我。"
    }
  ]
}
```

**智能体返回**：
```json
{
  "answer": "根据您的健康档案，您的血压指标为120/80 mmHg，属于正常范围。建议继续保持良好的生活习惯，定期监测血压。"
}
```

### 示例3：带健康数据的对话

**后端发送**：
```json
{
  "userId": 2,
  "question": "我的健康情况怎么样？",
  "userHealthData": {
    "userId": 2,
    "rawHealthDataList": [...],
    "healthArchives": [...]
  }
}
```

**智能体返回**：
```json
{
  "answer": "根据您的健康档案和体检报告，我为您分析如下：\n\n1. 整体健康状况：您的健康评分为75分，属于良好水平。\n\n2. 需要关注的指标：\n   - BMI指数为27.1，属于超重范围\n   - 甘油三酯为4.25 mmol/L，略高于正常值\n\n3. 建议：\n   - 控制饮食，减少高脂高糖食物\n   - 增加运动量，每周至少150分钟中等强度运动\n   - 定期复查相关指标\n\n注意：本分析仅基于现有数据，不能替代医生诊断。如有不适，请咨询专业医疗人员。"
}
```

---

## 五、常见问题

### Q1: 如果智能体接口路径不是 `/chat` 怎么办？

**A**: 在 `application.properties` 中配置完整路径：
```properties
ai.agent.chat.url=https://3c1d06ab.r30.cpolar.top/api/v1/chat
```

### Q2: 如果智能体接口需要GET方法怎么办？

**A**: 当前代码使用POST方法。如果需要GET方法，需要修改 `ConsultationServiceImpl.java` 中的 `HttpMethod.POST` 为 `HttpMethod.GET`，并将参数放在URL查询字符串中。

### Q3: 如果智能体返回的格式不在支持列表中怎么办？

**A**: 后端会尝试多种格式解析。如果都不匹配，会返回默认错误消息。建议智能体返回 `{"answer": "回答内容"}` 格式。

### Q4: 对话历史的最大长度是多少？

**A**: 前端会限制为最近10条消息，后端不做限制。建议智能体根据实际需要处理历史消息。

### Q5: userHealthData 字段很大，会影响性能吗？

**A**: 如果用户健康数据很大，可能会影响请求性能。建议：
1. 智能体只使用必要的数据
2. 或者后端优化，只发送关键数据
3. 或者使用分页或增量传输

---

## 六、调试建议

### 1. 查看后端日志

后端会记录以下信息：
- 请求URL
- 请求方法
- 请求头
- 请求体JSON（完整）
- 响应状态码
- 响应体

### 2. 测试智能体接口

可以使用以下工具测试智能体接口：
- Postman
- curl命令
- Apifox

**curl示例**：
```bash
curl -X POST https://3c1d06ab.r30.cpolar.top/chat \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 2,
    "question": "你好"
  }'
```

### 3. 检查URL路径

确保智能体接口的URL路径正确：
- 如果智能体接口是 `https://3c1d06ab.r30.cpolar.top/api/chat`，配置中应该写完整路径
- 如果智能体接口是 `https://3c1d06ab.r30.cpolar.top/chat`，配置中应该写完整路径

---

## 七、当前问题分析

根据错误日志：
```
智能体对话接口返回客户端错误，状态码：405 METHOD_NOT_ALLOWED，响应：{"detail":"Method Not Allowed"}
```

**可能的原因**：
1. **URL路径错误**：当前配置的URL是 `https://3c1d06ab.r30.cpolar.top`，可能缺少路径（如 `/chat`）
2. **HTTP方法错误**：智能体接口可能不支持POST方法，需要GET方法
3. **接口不存在**：该URL可能不是对话接口

**解决方案**：
1. 确认智能体接口的完整URL（包括路径）
2. 确认智能体接口支持的HTTP方法（GET/POST）
3. 在 `application.properties` 中配置正确的完整URL
