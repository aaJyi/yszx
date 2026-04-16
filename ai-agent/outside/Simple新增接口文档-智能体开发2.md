-# Simple新增接口文档 - 智能体开发

## 接口说明

本接口用于智能体将从小程序拍照的体检报告中提取的信息写入数据库。

---

## 接口信息

### 基本信息
- **接口路径**：`POST /simple/add`
- **请求方式**：`POST`
- **Content-Type**：`application/json`
- **接口用途**：新增体检报告基本信息

---

## 请求参数

### 路径参数
https://your-domain.com在开发阶段会每日更换，今日为https://b2b4151.r16.vip.cpolar.cn

### 查询参数（可选）
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Integer | 否 | 用户ID。如果不提供，系统会尝试从请求头中获取（需要用户已登录） |

### 请求体（JSON格式）

**请求体示例**：
```json
{
  "name": "张三",
  "gender": "男",
  "age": 35,
  "reportNumber": "TJ20240115001",
  "checkDate": "2024-01-15",
  "reportDate": "2024-01-16",
  "packageName": "基础体检套餐",
  "institutionName": "XX医院体检中心",
  "chiefDoctor": "李医生",
  "contactPhone": "021-12345678"
}
```

**字段说明**：

| 字段名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| name | String | 是 | 姓名 | "张三" |
| gender | String | 否 | 性别 | "男" / "女" |
| age | Integer | 否 | 年龄 | 35 |
| reportNumber | String | 否 | 体检编号 | "TJ20240115001" |
| checkDate | String | 否 | 检查日期（格式：yyyy-MM-dd） | "2024-01-15" |
| reportDate | String | 否 | 报告日期（格式：yyyy-MM-dd） | "2024-01-16" |
| packageName | String | 否 | 体检套餐 | "基础体检套餐" |
| institutionName | String | 否 | 机构名称 | "XX医院体检中心" |
| chiefDoctor | String | 否 | 主检医生 | "李医生" |
| contactPhone | String | 否 | 咨询电话 | "021-12345678" |

---

## 响应格式

### 成功响应

**HTTP状态码**：`200`

**响应体示例**：
```json
{
  "code": 200,
  "message": "体检报告信息保存成功",
  "data": {
    "id": 1,
    "userId": 2,
    "name": "张三",
    "reportNumber": "TJ20240115001",
    "checkDate": "2024-01-15",
    "reportDate": "2024-01-16"
  }
}
```

**响应字段说明**：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| code | Integer | 响应状态码，200表示成功 |
| message | String | 响应消息 |
| data | Object | 响应数据 |
| data.id | Integer | 保存后的记录ID（数据库自增） |
| data.userId | Integer | 用户ID |
| data.name | String | 姓名 |
| data.reportNumber | String | 体检编号 |
| data.checkDate | String | 检查日期 |
| data.reportDate | String | 报告日期 |

### 错误响应

#### 1. 用户ID为空

**HTTP状态码**：`400`

**响应体示例**：
```json
{
  "code": 400,
  "message": "用户ID不能为空，请提供userId参数或在请求头中设置用户信息",
  "data": null
}
```

#### 2. 姓名不能为空

**HTTP状态码**：`400`

**响应体示例**：
```json
{
  "code": 400,
  "message": "姓名不能为空",
  "data": null
}
```

#### 3. 服务器错误

**HTTP状态码**：`500`

**响应体示例**：
```json
{
  "code": 500,
  "message": "保存失败: [具体错误信息]",
  "data": null
}
```

---

## 调用示例

### Python示例

```python
import requests
import json

# 接口地址
url = "https://your-domain.com/simple/add?userId=2"

# 请求头
headers = {
    "Content-Type": "application/json"
}

# 请求体（从智能体提取的JSON数据）
data = {
    "name": "张三",
    "gender": "男",
    "age": 35,
    "reportNumber": "TJ20240115001",
    "checkDate": "2024-01-15",
    "reportDate": "2024-01-16",
    "packageName": "基础体检套餐",
    "institutionName": "XX医院体检中心",
    "chiefDoctor": "李医生",
    "contactPhone": "021-12345678"
}

# 发送请求
response = requests.post(url, headers=headers, json=data)

# 处理响应
if response.status_code == 200:
    result = response.json()
    if result["code"] == 200:
        print(f"保存成功，记录ID：{result['data']['id']}")
    else:
        print(f"保存失败：{result['message']}")
else:
    print(f"请求失败，状态码：{response.status_code}")
```

### cURL示例

```bash
curl -X POST "https://your-domain.com/simple/add?userId=2" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "张三",
    "gender": "男",
    "age": 35,
    "reportNumber": "TJ20240115001",
    "checkDate": "2024-01-15",
    "reportDate": "2024-01-16",
    "packageName": "基础体检套餐",
    "institutionName": "XX医院体检中心",
    "chiefDoctor": "李医生",
    "contactPhone": "021-12345678"
  }'
```

### JavaScript示例

```javascript
const url = 'https://your-domain.com/simple/add?userId=2';

const data = {
  name: "张三",
  gender: "男",
  age: 35,
  reportNumber: "TJ20240115001",
  checkDate: "2024-01-15",
  reportDate: "2024-01-16",
  packageName: "基础体检套餐",
  institutionName: "XX医院体检中心",
  chiefDoctor: "李医生",
  contactPhone: "021-12345678"
};

fetch(url, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify(data)
})
.then(response => response.json())
.then(result => {
  if (result.code === 200) {
    console.log('保存成功，记录ID：', result.data.id);
  } else {
    console.error('保存失败：', result.message);
  }
})
.catch(error => {
  console.error('请求失败：', error);
});
```

---

## 注意事项

1. **用户ID**：
   - 建议在URL参数中提供`userId`
   - 如果不提供，系统会尝试从请求头中获取（需要用户已登录）
   - 如果两种方式都无法获取，接口会返回400错误

2. **必填字段**：
   - `name`（姓名）是必填字段，不能为空

3. **日期格式**：
   - `checkDate`和`reportDate`使用`yyyy-MM-dd`格式，例如：`"2024-01-15"`

4. **数据验证**：
   - 接口会对必填字段进行验证
   - 如果验证失败，会返回400错误和具体错误信息

5. **错误处理**：
   - 建议在调用时处理各种错误情况
   - 根据返回的`code`和`message`进行相应的错误处理

6. **跨域支持**：
   - 接口已配置跨域支持（CORS），可以从任何域名调用

---

## 测试建议

1. **使用Postman或类似工具测试**：
   - 设置请求方法为POST
   - 设置URL为：`https://your-domain.com/simple/add?userId=2`
   - 设置请求头：`Content-Type: application/json`
   - 设置请求体为JSON格式

2. **测试用例**：
   - 正常情况：提供完整的JSON数据
   - 缺少必填字段：不提供`name`字段
   - 缺少用户ID：不提供`userId`参数

---

## 联系方式

如有问题，请联系：
- **邮箱**：tfxjp199@163.com
- **电话**：17821719161

---

**文档版本**：v1.0  
**最后更新**：2026年1月15日

![image-20260123103525250](C:\Users\XRDELL\AppData\Roaming\Typora\typora-user-images\image-20260123103525250.png)![image-20260123103644627](C:\Users\XRDELL\AppData\Roaming\Typora\typora-user-images\image-20260123103644627.png)
