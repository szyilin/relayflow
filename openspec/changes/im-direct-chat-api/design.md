# 设计：单聊 API（im-direct-chat-api）

## Context

- 契约真源：[`contract.md`](../../lanes/im-direct-chat/contract.md)
- 架构：[`im-platform-foundation`](../archive/2026-07-12-im-platform-foundation/design.md)

## Goals / Non-Goals

**Goals:**

- REST 三端点与 contract 对齐
- direct 会话 `(tenant_id, direct_peer_low, direct_peer_high)` 去重
- 发送：先持久化再 ACK；`client_msg_id` 幂等
- WS `message.send` 复用 `ImMessageService`，commit 后 `message.ack` + `message.new`

**Non-Goals:**

- 群聊/频道 API
- `read.report`、附件 multipart

## 分层

```text
controller/app/*Controller
    → ImConversationService / ImMessageService
        → Im*Mapper (codegen DO)
        → UserApi (昵称)
        → RealtimeTransportApi (message.new fanout)
websocket/ImDomainMessageHandler
    → ImMessageService
```

## direct 会话

- `low = min(self, peer)`，`high = max(self, peer)`
- 懒创建：发送时无 `conversationId` 则 `getOrCreateDirect`
- 列表 `title` / `avatarText`：运行时从 `UserApi.getUserBasic(peer)` 计算

## 发送与 seq

1. `SELECT ... FOR UPDATE` 锁定 `im_conversation`
2. 查 `client_msg_id` 幂等
3. `seq = max(seq)+1` 插入 `im_message`
4. 更新 `last_msg_*`；其他成员 `unread_count++`
5. REST 返回 ACK DTO；WS 额外 `sessionSender` 发 `message.ack`
6. `RealtimeTransportApi.sendToUsers` 推送 `message.new`

## 验证

```bash
./mvnw -pl relayflow-server -am compile
openspec validate im-direct-chat-api --strict
# curl 见 contract.md
```
