# API 契约：im-bot-dm（会话列表 bot_dm）

> **状态**：草案（`im-bot-notify-foundation` §6）  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **后端真源**：`ImBotApi` + `GET /app-api/im/conversation/list`（已返回 `type=bot_dm`）

## 背景

业务触达写入 `im_message`（`sender_type=bot`，会话 `bot_dm`）。工作台 `/app/messages` 须与单聊/群聊同一列表模型展示助手会话；未读走会话角标（无独立铃铛）。

## 鉴权

| 项 | 值 |
|----|-----|
| REST | `Authorization: Bearer <JWT>` |
| 产品面 | 有效组织成员（`/app-api`） |
| WebSocket | `/infra/ws` · `domain=im` |

## REST（沿用现有 IM 端点）

### GET /app-api/im/conversation/list

`data` 可含 `type=bot_dm` 项：

```json
{
  "id": "501",
  "type": "bot_dm",
  "title": "组织助手",
  "avatarText": "组",
  "lastMsgPreview": "张三 邀请你加入 Acme",
  "lastMsgAt": "2026-07-16T04:00:00Z",
  "unreadCount": 1,
  "botId": "900001",
  "botCode": "org-assistant"
}
```

| 字段 | 说明 |
|------|------|
| `title` / `avatarText` | 来自 `im_bot.name` |
| `botId` / `botCode` | 仅 `bot_dm` |
| `peerUserId` / `memberCount` | bot_dm 不返回 |

### GET /app-api/im/message/list

与人聊相同。Bot 出站消息：`senderType=bot`，`senderNickname` = Bot 显示名。

content blocks 可含 `type=deeplink`（`route` / `entityType` / `entityId`）；V1 UI 可先只渲染 text 块。

**卡片占位（地基 §8）**：`im_message.type` 与 content blocks 预留 `card`（及未来 `actions`）；地基期产方发 **text + deeplink** 即可。可交互 card callback（鉴权、超时、幂等）**不在地基实现**，见后续 `im-bot-interactive-card`；**禁止**回潮 `infra_notify` 双写。

### POST /app-api/im/message/send · read

用户可向 bot_dm 发文本；已读上报与人聊相同。

## 实时

`domain=im` · `type=message.new`：payload 为 `MessageItem`（含 bot 昵称）；前端按 `conversationId` 更新列表预览/未读。

## 前端路径

| 路径 | 行为 |
|------|------|
| `/app/messages` | 列表展示 bot_dm（`[助手]`、bot 图标） |
| `/app/messages?conversationId=` | 打开 bot_dm 历史；右侧栏展示助手信息（非在线状态） |

## curl 示例

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  "$BASE/app-api/im/conversation/list" | jq '.data[] | select(.type=="bot_dm")'
```

造数：管理端邀请已 ACTIVE 用户 → `org-assistant` 扇出 bot_dm（见 [im-bot-invite-migrate archive](../../../openspec/changes/archive/2026-07-16-im-bot-invite-migrate/proposal.md)）。
