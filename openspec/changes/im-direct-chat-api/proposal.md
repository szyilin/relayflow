# 提案：单聊 REST + WS Handler（im-direct-chat-api）

## Why

[`im-direct-chat-web`](../im-direct-chat-web/proposal.md) 已完成 `/app/messages` UI 与 [`contract.md`](../../lanes/im-direct-chat/contract.md)。`im-schema-v1` 与 `im-realtime-platform` 已就绪，须实现 **单聊 REST** 与 **`ImDomainMessageHandler`**，供 `-integrate` 去 Mock 联调。

## What Changes

- `im-biz`：`ImConversationService`、`ImMessageService`（持久化优先、seq、`client_msg_id` 幂等）
- App 控制器：`GET /app-api/im/conversation/list`、`GET /app-api/im/message/list`、`POST /app-api/im/message/send`
- `ImDomainMessageHandler`：`message.send` → DB → `message.ack` + `message.new` fanout
- `system-api`：扩展 `UserApi.getUserBasic`（会话列表展示对方昵称，禁止 im-biz 直连 `sys_*`）
- `im-api`：`ErrorCodeConstants`

## Capabilities

### Modified Capabilities

- `im`：落地单聊 REST、direct 会话去重、消息 seq/幂等、WS `im` 域 Handler

## Impact

| 区域 | 影响 |
|------|------|
| `relayflow-module-im-api` | 错误码 |
| `relayflow-module-im-biz` | Service、Controller、WS Handler |
| `relayflow-module-system-api` / `system-biz` | `UserApi.getUserBasic` |
| `web/` | **不改** |
| Flyway | **无** |

## 不在本 change

- 前端 WS 客户端（`-integrate`）
- 群聊/频道、已读上报、附件消息

## 前置

- 契约：[`openspec/lanes/im-direct-chat/contract.md`](../../lanes/im-direct-chat/contract.md)
- 表结构：`V0.1.0.6__init_im.sql`
- 传输层：`RealtimeTransportApi`、`RealtimeDomainMessageHandler`
