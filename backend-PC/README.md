# 快乐生活 - 管理后台

基于 Vue3 + Vite + Element Plus 的 PC 端管理后台，用于管理小程序各业务模块。

## 功能模块

| 模块     | 说明               |
|----------|--------------------|
| 工作台   | 概览与健康档案统计 |
| 社区活动 | 活动列表（分页）   |
| AI医生   | AI 医生列表        |

## 技术栈

- Vue 3
- Vite 5
- Vue Router 4
- Pinia
- Element Plus
- Axios

## 环境要求

- Node.js 18+
- 后端服务已启动（与小程序共用同一后端）

## 安装与运行

```bash
cd backend-PC
npm install
npm run dev
```

默认访问：http://localhost:5174

## 构建

```bash
npm run build
```

产物在 `dist/`，可部署到任意静态服务器。生产环境需配置后端 API 地址（见 `.env.production` 或 Nginx 反向代理 `/api`）。

## 登录说明

- 使用与小程序相同的 **手机号 + 密码** 登录（调用后端 `/t-user/login`）。
- 建议为管理员单独注册账号并在后端做权限区分（后续可扩展管理员角色与后台专用接口）。

## 接口说明

- 开发环境下，请求 `/api` 会通过 Vite 代理转发到 `http://1.14.191.118:8080`（与 `backend-mini/utils/config.js` 中 baseUrl 一致）。
- 若后端地址变更，请修改 `vite.config.js` 中 `server.proxy['/api'].target`，或配置 `.env.development` / `.env.production` 中的 `VITE_API_BASE_URL`。

## 后续扩展建议

- 后端增加管理员角色与 `/admin/*` 接口，实现活动、商品、医生等的新增/编辑/删除。
- 预约/订单列表增加“全部列表”接口（不按 userId 过滤），便于运营查看与导出。
