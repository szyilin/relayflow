# 任务：单聊联调（im-direct-chat-integrate）

> **Lane**：`*-integrate` · 契约 [`contract.md`](../../lanes/im-direct-chat/contract.md)

## 1. 前端联调

- [x] 1.1 `vite.config.ts` 代理 `/app-api`、`/infra/ws`
- [x] 1.2 `api/app/im.ts` ID 归一化
- [x] 1.3 `useImWebSocket` 真实连接 + `message.new` 回调
- [x] 1.4 `stores/im.ts` 去 Mock；REST + WS 事件处理
- [x] 1.5 `messages/index.vue` 接入 WS、移除 Mock 角标

## 2. 验证与看板

- [x] 2.1 `./mvnw -pl relayflow-server -am compile`
- [x] 2.2 `cd web && pnpm build`
- [x] 2.3 更新 `docs/dev/api-integration-board.md` → **done**
- [x] 2.4 `openspec validate im-direct-chat-integrate --strict`
