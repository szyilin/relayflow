# API 契约：im-direct-chat

> **状态**：草案（`-web` lane）  
> **起草**：`im-direct-chat-web` change  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **架构真源**：[`im-platform-foundation` design](../../changes/archive/2026-07-12-im-platform-foundation/design.md)

## 背景

员工工作台 `/app/messages` 单聊 MVP：会话列表、历史消息、发送文本消息；实时推送由 WebSocket 补充（`-integrate` 联调）。

## 鉴权

| 项 | 值 |
|----|-----|
| REST | `Authorization: Bearer <JWT>` |
| 产品面 | 有效组织成员（`/app-api`，不用 `sys_permission`） |
| WebSocket | `ws://host/infra/ws?token=<JWT>` |

## REST 端点

### GET /app-api/im/conversation/list

当前用户的会话列表（V1 仅 `direct`）。

**Query**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `keyword` | string | 可选；匹配 `title`、`lastMsgPreview` |

**Response `data`**：`ConversationItem[]`

```json
[
  {
    "id": "201",
    "type": "direct",
    "title": "李晓明",
    "avatarText": "李",
    "lastMsgPreview": "下午的评审会别忘了带原型",
    "lastMsgAt": "2026-07-12T08:00:00Z",
    "unreadCount": 2,
    "peerUserId": "102"
  }
]
```

### GET /app-api/im/message/list

**Query**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `conversationId` | string | 是 | 会话 ID |
| `afterSeq` | long | 否 | 默认 0；返回 `seq > afterSeq` |

**Response `data`**：`MessageItem[]`（按 `seq` 升序）

```json
[
  {
    "id": "301",
    "conversationId": "201",
    "senderId": "102",
    "senderType": "user",
    "type": "text",
    "content": {
      "version": 1,
      "blocks": [{ "type": "text", "text": "你好" }]
    },
    "clientMsgId": null,
    "seq": 1,
    "createTime": "2026-07-12T08:00:00Z"
  }
]
```

### POST /app-api/im/message/send

先持久化再返回 ACK（规格要求）。V1 仅 `text`。

**Request**：

```json
{
  "conversationId": "201",
  "peerUserId": "102",
  "clientMsgId": "550e8400-e29b-41d4-a716-446655440000",
  "type": "text",
  "content": {
    "version": 1,
    "blocks": [{ "type": "text", "text": "收到" }]
  }
}
```

| 字段 | 说明 |
|------|------|
| `conversationId` | 已有会话时必填 |
| `peerUserId` | 无会话时与 `conversationId` 二选一；懒创建 direct 会话 |
| `clientMsgId` | 客户端 UUID，幂等键 |

**Response `data`**：

```json
{
  "id": "302",
  "conversationId": "201",
  "seq": 2,
  "clientMsgId": "550e8400-e29b-41d4-a716-446655440000",
  "createTime": "2026-07-12T08:01:00Z"
}
```

重复 `clientMsgId` 返回已有消息，不重复插入。

## WebSocket Envelope（`-integrate`）

统一外壳见平台设计。单聊相关 type：

| 方向 | domain | type |
|------|--------|------|
| ↑ | im | `message.send` |
| ↓ | im | `message.ack` |
| ↓ | im | `message.new` |
| ↑↓ | system | `ping` / `pong` |

**上行 `message.send` payload** 与 REST send body 同构。

**下行 `message.ack`**：在 DB commit 后返回，含 `id`、`seq`、`clientMsgId`。

**下行 `message.new`**：推送给会话其他在线成员。

## curl 示例

```bash
TOKEN="<jwt>"

curl -s "http://localhost:8080/app-api/im/conversation/list" \
  -H "Authorization: Bearer $TOKEN"

curl -s "http://localhost:8080/app-api/im/message/list?conversationId=201&afterSeq=0" \
  -H "Authorization: Bearer $TOKEN"

curl -s -X POST "http://localhost:8080/app-api/im/message/send" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"201","clientMsgId":"'"$(uuidgen)"'","content":{"version":1,"blocks":[{"type":"text","text":"你好"}]}}'
```

## 前端映射

| UI | Store | API |
|----|-------|-----|
| `/app/messages` | `stores/im.ts` | `api/app/im.ts` |
| Mock 回退 | store 内 `isApiUnavailable` | `mocks/im.ts`（仅 store 引用） |

## V1 不在范围

- 群聊 / 频道 UI
- 图片 / 文件消息 UI
- 通知中心
- 已读回执 UI（`read.report` 可后续补）
