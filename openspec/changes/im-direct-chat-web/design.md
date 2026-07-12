# 设计：单聊工作台 UI（im-direct-chat-web）

## Context

- 契约真源：本 change 起草的 [`contract.md`](../../lanes/im-direct-chat/contract.md)
- UI 真源：[`workspace-ui-patterns.md`](../../../docs/dev/workspace-ui-patterns.md)

## Goals / Non-Goals

**Goals:**

- 会话列表 + 搜索 + 未读角标
- 消息气泡（自己/对方）+ 发送中/失败状态
- Store 调 API，404/网络失败 → Mock
- `pnpm build` 通过

**Non-Goals:**

- 真实 WebSocket（`-integrate`）
- 群聊/频道、附件、已读 UI

## 文件

```text
web/src/
├── api/app/im.ts
├── mocks/im.ts              # 仅 stores/im.ts 引用
├── stores/im.ts
├── composables/useImWebSocket.ts  # 占位
└── pages/app/messages/index.vue
```

## 验证

```bash
cd web && pnpm build
# 浏览器：/app/login → /app/messages（无后端时显示 Mock 角标）
```
