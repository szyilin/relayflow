# 任务：群聊联调（im-group-chat-integrate）

> **Lane**：`*-integrate` · 契约 [`contract.md`](../../lanes/im-group-chat/contract.md)

## 1. 前端联调

- [x] 1.1 `api/app/im.ts` 群聊 ID 归一化
- [x] 1.2 `stores/im.ts` 去群聊 Mock 回退
- [x] 1.3 群消息 REST + 现有 WS `message.new` 联调

## 2. 验证与看板

- [x] 2.1 `./mvnw -pl relayflow-server -am compile`
- [x] 2.2 `cd web && pnpm build`
- [x] 2.3 更新 `docs/dev/api-integration-board.md` → **done**
- [x] 2.4 `openspec validate im-group-chat-integrate --strict`
