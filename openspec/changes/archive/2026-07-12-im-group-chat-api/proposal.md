# 提案：群聊 REST + 系统消息（im-group-chat-api）

## Why

[`im-group-chat-web`](../im-group-chat-web/proposal.md) 已完成群聊 UI 与 [`contract.md`](../../lanes/im-group-chat/contract.md)。`im-schema-v1` 与单聊 API 已就绪，须实现 **群聊 REST**、会话列表 group 扩展、系统消息与 WS fanout，供 `-integrate` 去 Mock 联调。

## What Changes

- `ImGroupService`：建群、邀请成员、成员列表
- App 控制器：`POST /app-api/im/group/create`、`POST …/members/add`、`GET …/members`
- 扩展 `ImConversationService.listConversations`：返回 `type=group` 条目（含 `memberCount`）
- 扩展 `ImMessageService`：群消息 `senderNickname`；`sendSystemMessage`（成员加入）
- `ErrorCodeConstants` 群聊错误码

## Capabilities

### Modified Capabilities

- `im`：落地群聊 REST、系统消息、会话列表 group 扩展

## Impact

| 区域 | 影响 |
|------|------|
| `relayflow-module-im-api` | 错误码 |
| `relayflow-module-im-biz` | Group Service、Controller、Conversation/Message 扩展 |
| `web/` | **不改** |
| Flyway | **无** |

## 不在本 change

- 前端去 Mock（`im-group-chat-integrate`）
- 频道、踢人/退群、附件、已读 UI

## 前置

- 契约：[`openspec/lanes/im-group-chat/contract.md`](../../lanes/im-group-chat/contract.md)
- UI：`im-group-chat-web` ui_ready
