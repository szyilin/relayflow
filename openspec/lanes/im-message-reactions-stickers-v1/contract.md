# API 契约：im-message-reactions-stickers-v1

> **状态**：`web: done`；`api: ready`（`im-message-reactions-stickers-v1` 已联调，store 无 Mock）  
> **页面**：`/app/messages`

## 目标

消息表情回应、单条转发、系统表情包发送。

**不在范围**：租户自定义表情包上传、批量转发、卡片转发、Bot 自动 reaction。

## 鉴权

Bearer JWT；调用方须为相关会话成员。

## REST

### `POST /app-api/im/message/reaction/toggle`

```json
{ "messageId": "…", "emoji": "👍" }
```

**响应** `data`：

```json
{
  "messageId": "…",
  "conversationId": "…",
  "reactions": [{ "emoji": "👍", "count": 2, "me": true }]
}
```

规则：toggle；已撤回拒绝；emoji NFC + 长度 ≤32；每用户每消息最多 20 种。

### `POST /app-api/im/message/forward`

```json
{
  "messageId": "…",
  "targetConversationId": "…",
  "clientMsgId": "uuid-optional"
}
```

**响应** `data`：目标会话新消息（同 `MessageItem`，含 `forwardedFrom`）。

拒绝：已撤回、`type=card`、无目标发送权。

### `GET /app-api/im/sticker/packs`

**响应** `data`：

```json
[{
  "id": "…",
  "code": "system_default",
  "name": "系统表情",
  "scope": "system",
  "stickers": [{
    "id": "…",
    "packId": "…",
    "code": "ok",
    "name": "OK",
    "downloadUrl": "/app-api/infra/file/download?fileId=…"
  }]
}]
```

### `POST /app-api/im/message/send`（扩展）

```json
{
  "conversationId": "…",
  "clientMsgId": "…",
  "type": "sticker",
  "content": {
    "version": 1,
    "blocks": [{ "type": "sticker", "stickerId": "…" }]
  }
}
```

服务端补全 `packId` / `fileId`；列表填充 `downloadUrl`。

## 消息列表字段增量

`MessageItem` 增加：

| 字段 | 说明 |
|------|------|
| `reactions?` | `{ emoji, count, me }[]` |
| `forwardedFrom?` | `{ conversationId, messageId, senderNickname?, preview? }` |
| `type` | 增加 `sticker` |

## WebSocket

| type | payload |
|------|---------|
| `reaction.updated` | `{ conversationId, messageId, reactions }` 全量聚合快照 |
| `message.new` | 转发 / sticker 与普通消息相同信封 |

## 表（api 实现）

- `im_message_reaction`
- `im_sticker_pack` / `im_sticker`
- `im_message.type` CHECK 含 `sticker`

## 冒烟（联调后）

1. A 对消息点 👍 → B 实时看到 chip  
2. 再点取消 → 聚合更新  
3. 转发文本到另一会话 → 目标出现「转发自」条  
4. 发送系统 sticker → 气泡大图/glyph  
5. 撤回消息无法 reaction / forward；card 菜单无转发
