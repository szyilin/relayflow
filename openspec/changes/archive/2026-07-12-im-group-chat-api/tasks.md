# 任务：群聊 API（im-group-chat-api）

> **Lane**：`*-api` · 契约真源 [`contract.md`](../../lanes/im-group-chat/contract.md)

## 1. 前置

- [x] 1.1 `proposal.md` / `design.md` / spec delta / 本 `tasks.md`
- [x] 1.2 阅读 contract 与单聊实现

## 2. im-api

- [x] 2.1 `ErrorCodeConstants` 群聊错误码

## 3. im-biz

- [x] 3.1 `ImGroupService`：create、addMembers、listMembers
- [x] 3.2 `ImGroupController` 三端点 + VO
- [x] 3.3 `ImConversationService.listConversations` 扩展 group + memberCount
- [x] 3.4 `ImMessageService.sendSystemMessage` + listMessages senderNickname
- [x] 3.5 `ImConversationType.GROUP`

## 4. 验证与看板

- [x] 4.1 `./mvnw -pl relayflow-server -am compile`
- [x] 4.2 `openspec validate im-group-chat-api --strict`
- [x] 4.3 更新 [`api-integration-board.md`](../../../docs/dev/api-integration-board.md) → API `ready`

## 下一 change

- `im-group-chat-integrate` — 前端去 Mock + 群 WS 联调
