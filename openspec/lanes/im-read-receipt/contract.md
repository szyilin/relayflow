# API 契约：im-read-receipt

> **状态**：已联调（integrate done）  
> **起草**：`im-read-receipt-web` change  
> **基线**：[`im-direct-chat`](../im-direct-chat/contract.md)（`POST /conversation/read` 已存在）

## 背景

已读上报 API 已实现（`markConversationRead`），但发送方 UI 无法得知对方是否已读。本契约补充 **已读水位查询** 与 **WS 已读推送**，供单聊/群聊（V1 单聊展示「已读」，群聊可选不展示 per-user 已读）。

## 鉴权

Bearer JWT；须为会话成员。

## GET /app-api/im/conversation/read-status

查询会话内各成员（或单聊对端）的已读水位。

**Query**：

| 参数 | 必填 | 说明 |
|------|------|------|
| `conversationId` | 是 | 会话 ID |

**Response `data`**：

```json
{
  "conversationId": "201",
  "members": [
    { "userId": "100", "readSeq": 12 },
    { "userId": "102", "readSeq": 10 }
  ]
}
```

| 字段 | 说明 |
|------|------|
| `readSeq` | 该成员已读到的最大 seq（来自 `im_conversation_member.read_seq`） |

**规则**：

- 仅返回同会话成员
- 不暴露非成员信息

## POST /app-api/im/conversation/read（已有）

保持不变；成功后 **fanout** WS（见下）。

## WebSocket

### 下行：`read.updated`

当某成员调用 `markConversationRead` 成功后，向会话内其他在线成员推送：

```json
{
  "domain": "im",
  "type": "read.updated",
  "requestId": "...",
  "ts": 1710000000000,
  "payload": {
    "conversationId": "201",
    "userId": "102",
    "readSeq": 12
  }
}
```

### 前端行为

- 单聊：自己的消息 `seq <= peerReadSeq` 时显示「已读」
- 收到 `read.updated` 更新本地 peerReadSeq，无需全量刷新

## curl

```bash
curl -s "http://localhost:8080/app-api/im/conversation/read-status?conversationId=201" \
  -H "Authorization: Bearer $TOKEN"
```

## V1 不在范围

- 群聊「已读 N 人」列表
- 消息级双蓝勾（WhatsApp 风格）以外的复杂状态机
