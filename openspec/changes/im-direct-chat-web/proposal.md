# 提案：单聊工作台 UI（im-direct-chat-web）

## Why

`im-schema-v1` 与 `im-realtime-platform` 已就绪，但 `/app/messages` 仍为占位空壳。须按 **前端优先** 完成单聊 UI + Mock + API 契约，供 `im-direct-chat-api` 按 contract 实现。

## What Changes

- `/app/messages`：会话列表、聊天气泡、发送框（乐观 UI + `clientMsgId`）
- `stores/im.ts`、`api/app/im.ts`、store 内 Mock 回退
- `openspec/lanes/im-direct-chat/contract.md` 草案
- `auth` store 增加 `userId`（来自 permission-info）

## Capabilities

### Modified Capabilities

- `im`：用户端单聊 UI 与 REST 契约草案（行为在 `-api` 实现）

## Impact

| 区域 | 影响 |
|------|------|
| `web/` | messages 页、store、api、mocks |
| Java | **不改** |
| 看板 | `im-direct-chat` web → ui_ready |
