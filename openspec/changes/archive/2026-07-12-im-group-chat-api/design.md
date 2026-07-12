# 设计：群聊 API（im-group-chat-api）

## Context

- 契约：[`contract.md`](../../lanes/im-group-chat/contract.md)
- 单聊实现：[`im-direct-chat-api`](../archive/2026-07-12-im-direct-chat-api/design.md)
- 表：`im_conversation`、`im_group`、`im_conversation_member`、`im_message`

## Goals

- 建群：创建 `im_conversation(type=group)` + `im_group` + 成员（owner + 初始成员）
- 邀请：追加 `im_conversation_member`；去重已存在成员
- 系统消息：`sender_type=system`，文案「{nickname} 加入了群聊」
- 会话列表：direct + group 混排；group 带 `memberCount`
- 消息列表：user 消息带 `senderNickname`（群聊展示）
- 群消息发送：复用现有 `POST /message/send`（已支持 `conversationId`）
- WS：`message.new` fanout 至群在线成员（系统消息与用户消息同路径）

## 服务划分

```text
ImGroupController
  → ImGroupService（建群/邀请/成员列表）
  → ImMessageService.sendSystemMessage（系统消息）
ImConversationController（已有）
  → ImConversationService.listConversations（扩展 group）
ImMessageController（已有）
  → ImMessageService（扩展 senderNickname）
```

## 成员 role

| role | 场景 |
|------|------|
| `owner` | 建群者 |
| `member` | 被邀请成员 |

## 验证

```bash
./mvnw -pl relayflow-server -am compile
openspec validate im-group-chat-api --strict
```

curl 见 contract.md。
