# API 契约：im-group-chat

> **状态**：草案（`-web` lane）  
> **起草**：`im-group-chat-web` change  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **架构真源**：[`im-platform-foundation` design](../../changes/archive/2026-07-12-im-platform-foundation/design.md)  
> **单聊基线**：[`im-direct-chat`](../im-direct-chat/contract.md)

## 背景

员工工作台 `/app/messages` 群聊 MVP：建群、邀请成员、群消息、系统消息（成员加入）。复用统一 `im_conversation` + `im_group` + `im_message` 模型。

## 鉴权

| 项 | 值 |
|----|-----|
| REST | `Authorization: Bearer <JWT>` |
| 产品面 | 有效组织成员 + 须为会话成员方可读消息/发消息 |
| WebSocket | 与单聊相同；`message.new` fanout 至群在线成员（`-api` / `-integrate`） |

## REST 端点

### POST /app-api/im/group/create

创建群聊；创建者为 `owner`，初始成员一并加入。

**Request**：

```json
{
  "name": "产品讨论组",
  "memberUserIds": ["102", "103"]
}
```

| 字段 | 说明 |
|------|------|
| `name` | 群名称，1–128 字符 |
| `memberUserIds` | 邀请的成员用户 ID 列表（不含创建者；至少 1 人） |

**Response `data`**：

```json
{
  "conversationId": "301",
  "groupId": "401"
}
```

**副作用（`-api`）**：

- 插入 `im_conversation`（`type=group`）、`im_group`、`im_conversation_member`（owner + members）
- 可选：插入 system 消息「XXX 加入了群聊」（创建时对各初始成员）

### POST /app-api/im/group/members/add

向已有群追加成员（须为当前群成员且具备邀请权限；V1 MVP：任意成员可邀请）。

**Request**：

```json
{
  "conversationId": "301",
  "memberUserIds": ["104"]
}
```

**Response `data`**：`null` 或 `{ "addedCount": 1 }`

**副作用**：为新成员插入 `im_conversation_member`；插入 system 消息「{nickname} 加入了群聊」。

### GET /app-api/im/group/members

**Query**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `conversationId` | string | 是 | 群会话 ID |

**Response `data`**：`GroupMemberItem[]`

```json
[
  {
    "userId": "100",
    "nickname": "张三",
    "avatarText": "张",
    "role": "owner"
  },
  {
    "userId": "102",
    "nickname": "李晓明",
    "avatarText": "李",
    "role": "member"
  }
]
```

| `role` | 说明 |
|--------|------|
| `owner` | 群主 |
| `admin` | 管理员（V1 可选） |
| `member` | 普通成员 |

### GET /app-api/im/conversation/list（扩展）

在单聊契约基础上，`type=group` 条目增加字段：

```json
{
  "id": "301",
  "type": "group",
  "title": "产品讨论组",
  "avatarText": "产",
  "memberCount": 4,
  "lastMsgPreview": "收到",
  "lastMsgAt": "2026-07-12T10:00:00Z",
  "unreadCount": 0
}
```

### GET /app-api/im/message/list（群聊）

与单聊相同 Query/Response。群聊消息可增加 `senderNickname` 供 UI 展示：

```json
{
  "id": "501",
  "conversationId": "301",
  "senderId": "102",
  "senderNickname": "李晓明",
  "senderType": "user",
  "type": "text",
  "content": {
    "version": 1,
    "blocks": [{ "type": "text", "text": "大家好" }]
  },
  "seq": 2,
  "createTime": "2026-07-12T10:01:00Z"
}
```

**系统消息**：

```json
{
  "id": "502",
  "conversationId": "301",
  "senderId": "0",
  "senderType": "system",
  "type": "system",
  "content": {
    "version": 1,
    "blocks": [{ "type": "text", "text": "李晓明 加入了群聊" }]
  },
  "seq": 3,
  "createTime": "2026-07-12T10:02:00Z"
}
```

### POST /app-api/im/message/send（群聊）

与单聊相同；群聊 MUST 传 `conversationId`，不传 `peerUserId`。

```json
{
  "conversationId": "301",
  "clientMsgId": "550e8400-e29b-41d4-a716-446655440000",
  "content": {
    "version": 1,
    "blocks": [{ "type": "text", "text": "收到" }]
  }
}
```

## WebSocket（`-integrate`）

与单聊共用 `message.send` / `message.ack` / `message.new`；群消息 fanout 至所有在线群成员（除发送者）。

## curl 示例

```bash
TOKEN="<jwt>"

curl -s -X POST "http://localhost:8080/app-api/im/group/create" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"产品讨论组","memberUserIds":["102","103"]}'

curl -s "http://localhost:8080/app-api/im/group/members?conversationId=301" \
  -H "Authorization: Bearer $TOKEN"

curl -s -X POST "http://localhost:8080/app-api/im/group/members/add" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"301","memberUserIds":["104"]}'

curl -s -X POST "http://localhost:8080/app-api/im/message/send" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"301","clientMsgId":"'"$(uuidgen)"'","content":{"version":1,"blocks":[{"type":"text","text":"大家好"}]}}'
```

## 前端映射

| UI | Store | API |
|----|-------|-----|
| 建群弹窗 | `im.createGroup()` | `POST …/group/create` |
| 邀请弹窗 | `im.inviteGroupMembers()` | `POST …/group/members/add` |
| 成员侧栏 | `im.groupMembers` | `GET …/group/members` |
| 群消息 | `im.sendText()` | `POST …/message/send` |

Mock 回退：仅 store 引用 `mocks/im.ts`；页面不得 import。

## V1 不在范围

- 频道 UI/API
- 踢人、退群、转让群主
- 附件、已读 UI、@提及
- 群公告编辑
