# 提案：已读回执 UI（im-read-receipt-web）

## Why

`POST /app-api/im/conversation/read` 与 store 内 `reportConversationRead` 已清除未读角标，但发送方消息气泡无「已读」反馈（飞书/微信式体验）。须按 **前端优先** 完成已读 UI + 契约扩展草案，供 `im-read-receipt-api` 实现 read-status 与 WS `read.updated`。

## What Changes

- `/app/messages` 单聊：自己发送的消息下方显示「已读」（当对端 readSeq ≥ msg.seq）
- `stores/im.ts`：`peerReadSeqByConversation`、WS 监听 `read.updated`
- 起草 `openspec/lanes/im-read-receipt/contract.md`
- Mock：API 未就绪时用固定 peerReadSeq 演示

## Capabilities

### Modified Capabilities

- `im`：已读回执 UI 与 read-status 契约草案

## Impact

| 区域 | 影响 |
|------|------|
| `web/` | messages 页、im store、WS composable |
| Java | **不改** |

## 不在本 change

- `read-status` REST、WS fanout（`im-read-receipt-api`）
- 群聊 per-user 已读列表

## 下一 change

- `im-read-receipt-api`
