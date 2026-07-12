# 任务：单聊工作台 UI（im-direct-chat-web）

> **Lane**：`*-web` 第一步。后端见 `im-direct-chat-api`。

## 1. 前置

- [x] 1.1 阅读 [`contract.md`](../../lanes/im-direct-chat/contract.md) 与 `design.md`

## 2. 前端（web/）

- [x] 2.1 `api/app/im.ts`：会话列表、消息列表、发送 API 类型
- [x] 2.2 `stores/im.ts` + `mocks/im.ts`（store 内 `isApiUnavailable` 回退）
- [x] 2.3 `/app/messages`：列表、气泡、发送、Mock 角标
- [x] 2.4 `composables/useImWebSocket.ts` 占位；`auth` store 增加 `userId`
- [x] 2.5 更新 [`api-integration-board.md`](../../../docs/dev/api-integration-board.md)

## 3. 验证

- [x] 3.1 `cd web && pnpm build`
- [x] 3.2 `openspec validate im-direct-chat-web --strict`

## 浏览器路径

1. `pnpm dev` → `/app/login` 登录
2. `/app/messages` — 无后端时左上角 Mock 角标，可发消息

## 下一 change

- `im-direct-chat-api` — 按 contract 实现 REST + `ImDomainMessageHandler`
