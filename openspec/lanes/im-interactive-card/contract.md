# API 契约：im-interactive-card（可交互卡片）

> **状态**：冻结（`im-bot-interactive-card`）  
> **真源约定**：[`docs/dev/im-bot-interactive-card.md`](../../../docs/dev/im-bot-interactive-card.md)  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)

## 背景

Card 是 IM 交互协议；业务域经 `ImBotApi.send` 发卡，用户点击经统一 `POST /app-api/im/card/action` → 进程内 `CardActionHandler`。与 Bot Runtime 对话入站分离。系统 Bot **禁止**回调 URL。

## 鉴权

| 项 | 值 |
|----|-----|
| REST | `Authorization: Bearer <JWT>` |
| 产品面 | 有效组织成员（`/app-api`） |
| Action | 仅会话 **User** 成员可操作 |

## 消息存放

- `im_message.type` = `card`
- `content_json.blocks[]` 含 `type=card` 块（V1 模板 `generic.v1`）

### Card block（`generic.v1`）— 成员邀请示例

```json
{
  "type": "card",
  "schema": 1,
  "cardId": "c_invite_42",
  "template": "generic.v1",
  "header": {
    "title": "邀请加入企业",
    "subtitle": "Acme"
  },
  "fields": [
    { "label": "邀请人", "value": "张三" },
    { "label": "企业", "value": "Acme" }
  ],
  "actions": [
    {
      "id": "accept",
      "label": "接受邀请",
      "style": "primary",
      "behavior": {
        "type": "callback",
        "actionKey": "system.member.invite.accept",
        "payload": {
          "tenantId": "42",
          "tenantName": "Acme"
        }
      }
    }
  ],
  "meta": {
    "expiresAt": "2030-01-01T00:00:00Z",
    "source": {
      "domain": "system",
      "entityType": "tenant_invite",
      "entityId": "42"
    }
  }
}
```

| `behavior.type` | 前端 | 后端 |
|-----------------|------|------|
| `open_url` | 本地 `router.push(route)` / 外链 | **不**调 action API |
| `callback` | `POST …/card/action`；有 `form` 时先收集 `formValues` | SPI Handler |

## REST

### POST /app-api/im/card/action

```json
{
  "messageId": "1001",
  "conversationId": "501",
  "actionId": "accept",
  "actionKey": "system.member.invite.accept",
  "payload": { "tenantId": "42", "tenantName": "Acme" },
  "clientActionId": "uuid"
}
```

成功：

```json
{
  "code": 0,
  "data": {
    "toast": { "type": "success", "content": "已加入「Acme」，可在左下角切换企业" },
    "message": { /* 换卡后的 MessageItem；未换卡可省略 */ }
  }
}
```

| 错误场景 | 行为 |
|----------|------|
| 非成员 | 拒绝 |
| `meta.expiresAt` 已过 | 拒绝，不调 Handler |
| 同 `clientActionId` 重试 | 返回首次成功结果，不二次副作用 |
| 未注册 `actionKey` | 拒绝 |

## 业务：成员邀请卡

管理员 `POST /admin-api/system/user/invite` 成功且受邀人已有 ACTIVE 企业时，`org-assistant` 向其 ACTIVE 租户 bot_dm 发 `generic.v1` 卡：

- `actionKey`：`system.member.invite.accept`
- `payload`：`{ tenantId, tenantName }`（邀请企业）
- 点击「接受邀请」→ `NOT_JOINED` → `ACTIVE`；换卡为「已加入企业」

无 ACTIVE 企业时仍不发卡（注册页 pending banner）。

## 实时

| type | 说明 |
|------|------|
| `message.new` | 发卡 |
| `message.updated` | 换卡后 patch；payload = `MessageItem` |

`domain=im`。**禁止** `domain=notify` / 写 `infra_notify`。

## 前端路径

| 路径 | 行为 |
|------|------|
| `/app/messages` | 渲染 `generic.v1`；`open_url` 本地跳转；`callback` 调统一 API；过期禁用；toast / 换卡 |
