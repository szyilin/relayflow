# 设计：IM 深化 V1 路线图（im-deepening-v1）

## 架构基线

- 平台设计：[`im-platform-foundation`](../archive/2026-07-12-im-platform-foundation/design.md)
- 主 spec：[`openspec/specs/im/spec.md`](../../specs/im/spec.md)
- 工作流：[`frontend-first-workflow.md`](../../../docs/dev/frontend-first-workflow.md)

## 切片依赖图

```text
im-group-chat-* (done)
       │
       ▼
im-message-file-web (done)
       │
       ▼
im-message-file-api ──► im-message-file-integrate
       │
       ▼
im-read-receipt-web ──► im-read-receipt-api（可与 web 同会话联调）
       │
       ▼
im-presence-web ──► im-presence-api ──► im-presence-integrate
```

## 契约真源（永久）

| 切片 | contract |
|------|----------|
| 单聊 | [`lanes/im-direct-chat/contract.md`](../../lanes/im-direct-chat/contract.md) |
| 群聊 | [`lanes/im-group-chat/contract.md`](../../lanes/im-group-chat/contract.md) |
| 附件 | [`lanes/im-message-file/contract.md`](../../lanes/im-message-file/contract.md) |
| 已读 | [`lanes/im-read-receipt/contract.md`](../../lanes/im-read-receipt/contract.md) |
| 在线 | [`lanes/im-presence/contract.md`](../../lanes/im-presence/contract.md) |

## 会话粒度

- 每次 AI 会话：**一个子 change**、**≤10 条 tasks**
- `-web` 必须先 `pnpm build`；`-api` 必须先 `mvn compile` + curl

## 归档

各子 change 独立 archive；全部完成后 archive 本母 change 并同步 `openspec/specs/im/spec.md`。
