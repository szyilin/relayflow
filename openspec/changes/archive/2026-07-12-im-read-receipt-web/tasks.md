# 任务：已读回执 UI（im-read-receipt-web）

> **Lane**：`*-web` · 后端见 `im-read-receipt-api`

## 1. 前置

- [x] 1.1 `proposal.md` / `design.md` / spec delta / 本 `tasks.md`
- [x] 1.2 阅读 [`contract.md`](../../lanes/im-read-receipt/contract.md)

## 2. 前端（web/）

- [x] 2.1 起草 `openspec/lanes/im-read-receipt/contract.md`（若 web 未覆盖则补全）
- [x] 2.2 `api/app/im.ts`：`getReadStatus(conversationId)`
- [x] 2.3 `stores/im.ts`：peerReadSeq 状态
- [x] 2.4 `useImWebSocket.ts`：处理 `read.updated`
- [x] 2.5 `/app/messages`：单聊 outgoing「已读」标签
- [x] 2.6 更新看板

## 3. 验证

- [x] 3.1 `cd web && pnpm build`
- [x] 3.2 `openspec validate im-read-receipt-web --strict`

## 下一 change

- `im-read-receipt-api`
