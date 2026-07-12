# API 契约：org-member-invite-notify

> **状态**：草案（`-web` lane）  
> **起草**：`org-member-invite-notify` change  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)

## 背景

- 注册页：输入手机号后展示待加入企业（`pending`）
- 工作台 Rail：登录用户查看站内通知未读数与列表（`infra/notify`）

## 鉴权

| 端点 | 鉴权 |
|------|------|
| `GET .../member-invite/pending` | **permitAll** |
| `GET/POST .../infra/notify/*` | `Authorization: Bearer <JWT>` + 有效成员 |

## REST 端点

### GET /app-api/system/member-invite/pending

**Query**：`mobile`（11 位，可含空格，后端 normalize）

**Response `data`**：

```json
{
  "items": [
    {
      "tenantId": 2,
      "tenantName": "Acme 科技",
      "invitedAt": "2026-07-12T10:00:00+08:00"
    }
  ]
}
```

### GET /app-api/infra/notify/unread-count

**Response `data`**：

```json
{ "unreadCount": 3 }
```

### GET /app-api/infra/notify/page

**Query**：`pageNo`（默认 1）、`pageSize`（默认 20，最大 100）

**Response `data`**：

```json
{
  "list": [
    {
      "id": 9001,
      "tenantId": 2,
      "type": "MEMBER_INVITE",
      "title": "企业邀请",
      "body": "管理员 邀请你加入 Acme 科技",
      "payload": { "tenantName": "Acme 科技", "inviterNickname": "管理员" },
      "read": false,
      "createTime": "2026-07-12T10:00:00+08:00"
    }
  ],
  "total": 1
}
```

### POST /app-api/infra/notify/read

**Body**：

```json
{ "ids": [9001, 9002] }
```

**Response `data`**：`true`

## 前端页面

| 页面 / 组件 | 行为 |
|-------------|------|
| `/app/register` | mobile debounce → `pending` → `UAlert` 列表 |
| `WorkspaceNotifyBell` | 未读角标；`UModal` 列表；点击项 `markRead` |

## curl 示例

```bash
curl -s 'http://localhost:8080/app-api/system/member-invite/pending?mobile=13900001111' | jq

TOKEN=...
curl -s -H "Authorization: Bearer $TOKEN" \
  'http://localhost:8080/app-api/infra/notify/unread-count' | jq

curl -s -H "Authorization: Bearer $TOKEN" \
  'http://localhost:8080/app-api/infra/notify/page?pageNo=1&pageSize=20' | jq

curl -s -X POST -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"ids":[9001]}' 'http://localhost:8080/app-api/infra/notify/read' | jq
```

## 浏览器验证（integrate）

1. 管理端邀请手机号 B（`NOT_JOINED`）
2. `/app/register` 输入 B → 见邀请横幅
3. 注册/登录后工作台 Rail 铃铛有未读 → 打开列表可见 `MEMBER_INVITE`
