# 任务：已读回执 API（im-read-receipt-api）

> **Lane**：`*-api` · 契约 [`contract.md`](../../lanes/im-read-receipt/contract.md)

## 1. 前置

- [x] 1.1 `proposal.md` / `design.md` / spec delta / 本 `tasks.md`

## 2. im-biz

- [x] 2.1 `ConversationReadStatusRespVO` + `GET /read-status`
- [x] 2.2 `ImConversationService.getReadStatus()`
- [x] 2.3 `markConversationRead` 后 fanout `read.updated`
- [x] 2.4 `ImRealtimeTypes.READ_UPDATED`

## 3. 验证

- [x] 3.1 curl read-status + 双用户已读 WS 冒烟
- [x] 3.2 `./mvnw -pl relayflow-server -am compile`
- [x] 3.3 `openspec validate im-read-receipt-api --strict`
- [x] 3.4 看板更新

## 联调（可与 web 同会话）

- [x] 4.1 `im-read-receipt-web` store 接真实 API + WS `read.updated`
