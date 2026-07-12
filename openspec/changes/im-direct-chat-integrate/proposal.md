# 提案：单聊联调（im-direct-chat-integrate）

## Why

[`im-direct-chat-api`](../im-direct-chat-api/proposal.md) 已实现 REST 与 `ImDomainMessageHandler`；[`im-direct-chat-web`](../im-direct-chat-web/proposal.md) 的 store 仍 Mock 回退。须接入真实 API 与 `/infra/ws`，完成 `/app/messages` 端到端联调。

## What Changes

- `stores/im.ts`：去除 Mock 回退；REST 拉取/发送；处理 WS `message.new`
- `composables/useImWebSocket.ts`：连接 `/infra/ws?token=`，分发 `im` 域事件
- `vite.config.ts`：开发代理 `/app-api`、`/infra/ws`
- `api/app/im.ts`：Long ID 归一化为 string
- 看板 `im-direct-chat` → **done**

## Capabilities

### Modified Capabilities

- `im`：用户端单聊 store 无 Mock，REST + WS 实时收消息

## Impact

| 区域 | 影响 |
|------|------|
| `web/` | store、composable、vite proxy、messages 页 |
| Java | **不改** |

## 不在本 change

- 群聊/频道、已读 UI、附件
- archive `im-direct-chat-web` / `-api`（可后续批量 archive）

## 前置

- 契约：[`openspec/lanes/im-direct-chat/contract.md`](../../lanes/im-direct-chat/contract.md)
- API：`im-direct-chat-api` ready
