# API 契约：im-group-bot（G1 · 群挂载 Bot）

> **状态**：草案（`im-bot-group-member`）  
> **父契约**：[`im-group-chat`](../im-group-chat/contract.md)  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **规格**：`openspec/changes/im-bot-group-member/specs/im/spec.md`

## 背景

在已有群聊上挂载/移除系统 Bot；成员列表区分 User / Bot。不实现 @Bot、Runtime、卡片。

## 鉴权

| 项 | 值 |
|----|-----|
| REST | `Authorization: Bearer <JWT>` |
| 读成员 / 目录 | 须为群 User 成员 |
| 挂载 / 移除 Bot | **群主（owner）** |

## REST

### GET /app-api/im/group/members（扩展）

在原契约基础上，每项增加 `subjectType`；Bot 成员用 `botId`/`botCode`，`userId` 可省略。

```json
[
  {
    "subjectType": "user",
    "userId": "100",
    "nickname": "张三",
    "avatarText": "张",
    "role": "owner"
  },
  {
    "subjectType": "bot",
    "botId": "900003",
    "botCode": "task-bot",
    "nickname": "任务助手",
    "avatarText": "任",
    "role": "member"
  }
]
```

### GET /app-api/im/group/bots/catalog

可挂载的系统 Bot 目录（已启用、`type=system`），并标记是否已在群内。

**Query**：`conversationId`（必填）

**Response `data`**：

```json
[
  {
    "botId": "900003",
    "botCode": "task-bot",
    "name": "任务助手",
    "avatarText": "任",
    "alreadyMember": false
  }
]
```

### POST /app-api/im/group/bots/add

**Request**：

```json
{
  "conversationId": "301",
  "botCode": "task-bot"
}
```

**Response `data`**：

```json
{ "added": true }
```

幂等：已是成员时 `added=false`，HTTP 仍成功。

**副作用**：写入 `subject_type=bot` 成员；`sender_type=system` 文案「{botName} 加入了群聊」。

### POST /app-api/im/group/bots/remove

**Request**：

```json
{
  "conversationId": "301",
  "botCode": "task-bot"
}
```

**Response `data`**：

```json
{ "removed": true }
```

已不在群内时 `removed=false`。可选 system 文案「{botName} 离开了群聊」。

## 错误码

| code | 场景 |
|------|------|
| `GROUP_OWNER_REQUIRED` | 非群主挂载/移除 |
| `BOT_NOT_FOUND` | botCode 无效或停用 |
| `BOT_NOT_ENABLED_FOR_TENANT` | 非 system Bot 且未启用 |
| `GROUP_NOT_FOUND` / `CONVERSATION_ACCESS_DENIED` | 同现网 |

## curl

```bash
TOKEN="<jwt>"
CID=301

curl -s "http://localhost:8080/app-api/im/group/members?conversationId=$CID" \
  -H "Authorization: Bearer $TOKEN"

curl -s "http://localhost:8080/app-api/im/group/bots/catalog?conversationId=$CID" \
  -H "Authorization: Bearer $TOKEN"

curl -s -X POST "http://localhost:8080/app-api/im/group/bots/add" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"conversationId\":\"$CID\",\"botCode\":\"task-bot\"}"

curl -s -X POST "http://localhost:8080/app-api/im/group/bots/remove" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"conversationId\":\"$CID\",\"botCode\":\"task-bot\"}"
```

## 前端映射

| UI | Store | API |
|----|-------|-----|
| 成员侧栏 Bot 行 | `im.groupMembers` | `GET …/members` |
| 添加机器人 | `im.addGroupBot` | `POST …/bots/add` |
| 移除机器人 | `im.removeGroupBot` | `POST …/bots/remove` |
| 可选 Bot 列表 | `im.fetchGroupBotCatalog` | `GET …/bots/catalog` |

## V1 不在范围

- @Bot / Ingress / Runtime
- 外部 installable Bot
- 非群主挂载
