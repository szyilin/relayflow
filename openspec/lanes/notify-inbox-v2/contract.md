# API 契约：notify-inbox-v2

> **状态**：草案（母 change 规划；`-web` lane 实施时细化）  
> **起草**：`notify-inbox-v2` change  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **前置契约**：[`org-member-invite-notify/contract.md`](../org-member-invite-notify/contract.md)

## 背景

在 V1 邀请通知基础上扩展：

- 通知 **类型目录**（`TASK_DUE`、`APPROVAL_PENDING` 等）
- **dedupeKey** 幂等
- **按 type 筛选**、**全部标已读**
- **WebSocket** `domain=notify` / `type=notify.new` 实时刷新角标
- **deep link**：`payload.route` 点击跳转

## 鉴权

| 端点 | 鉴权 |
|------|------|
| `GET/POST .../infra/notify/*` | `Authorization: Bearer <JWT>` + 有效成员 |

## 类型常量（`InfraNotifyType`）

| type | 说明 | V2 生产方 |
|------|------|-----------|
| `MEMBER_INVITE` | 企业邀请 | system-biz（已有） |
| `TASK_DUE` | 任务将到期 | task-biz |
| `TASK_ASSIGNED` | 任务指派 | 预留 assign change |
| `IM_MENTION` | 消息 @我 | 预留 im-mention |
| `APPROVAL_PENDING` | 审批待办 | bpm-biz |

## REST 端点（增量）

### GET /app-api/infra/notify/page

**Query**：

| 参数 | 说明 |
|------|------|
| `pageNo` | 默认 1 |
| `pageSize` | 默认 20，最大 100 |
| `type` | **新增** 可选，如 `TASK_DUE` |

**Response `data.list[]` 项**（在 V1 基础上扩展）：

```json
{
  "id": 9001,
  "tenantId": 1,
  "type": "TASK_DUE",
  "title": "任务即将到期",
  "body": "「周报」将在 2026-07-13T18:00:00+08:00 到期",
  "payload": {
    "route": "/app/tasks?taskId=123",
    "entityType": "task",
    "entityId": "123",
    "dueTime": "2026-07-13T18:00:00+08:00"
  },
  "read": false,
  "createTime": "2026-07-13T10:00:00+08:00"
}
```

### GET /app-api/infra/notify/unread-count

**Response `data`**（兼容 V1）：

```json
{ "unreadCount": 5 }
```

**可选扩展**：

```json
{
  "unreadCount": 5,
  "byType": {
    "MEMBER_INVITE": 1,
    "TASK_DUE": 2,
    "APPROVAL_PENDING": 2
  }
}
```

### POST /app-api/infra/notify/read

不变，body `{ "ids": [9001] }`。

### POST /app-api/infra/notify/read-all

**新增**

**Body**（均可选）：

```json
{}
```

或按类型：

```json
{ "type": "TASK_DUE" }
```

**Response `data`**：`true`

## WebSocket envelope

连接：现有 `/infra/ws?token=...`（与 IM 共用或同连接）。

**下行**（服务端 → 客户端）：

```json
{
  "domain": "notify",
  "type": "notify.new",
  "payload": {
    "unreadCount": 6,
    "id": 9001,
    "type": "TASK_DUE",
    "title": "任务即将到期"
  }
}
```

**前端处理**：

- 收到后调用 `notifyStore.fetchUnreadCount()`
- 若铃铛 Modal 已打开，可选 `fetchInbox()`

## NotifyItemCommand（跨模块 push）

```json
{
  "tenantId": 1,
  "userId": 100,
  "mobile": null,
  "type": "TASK_DUE",
  "title": "任务即将到期",
  "body": "「周报」将在 … 到期",
  "dedupeKey": "task:123",
  "payload": {
    "route": "/app/tasks?taskId=123",
    "entityType": "task",
    "entityId": "123"
  }
}
```

- `dedupeKey` 可选；有则未读同 key 刷新而非新增行
- 业务模块 **只** 调 `NotifyInboxApi`；**不得**自行发 WS

## TASK_DUE 生产方（task-biz）

触发（见 design §D5）：

1. create/update：`due_time` 在 `[now, now+window]` 且 `status=TODO`
2. `pageMyTasks` lazy 补偿

配置：`relayflow.task.due-remind-window` 默认 `24h`

## 前端 UI（WorkspaceNotifyBell）

| 能力 | 说明 |
|------|------|
| 类型图标 | 见 design §D7 |
| 筛选 Chip | 全部 / 邀请 / 任务 / … |
| 全部已读 | 调 `read-all` |
| 点击项 | 标已读 + `router.push(payload.route)` |
| 空状态 | 「企业邀请、任务提醒等会显示在这里」 |

## curl 示例

```bash
# 按类型分页
curl -s 'http://localhost:8080/app-api/infra/notify/page?type=TASK_DUE&pageNo=1&pageSize=10' \
  -H "Authorization: Bearer $TOKEN" | jq

# 全部标已读
curl -s -X POST http://localhost:8080/app-api/infra/notify/read-all \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{}' | jq
```

## 浏览器验证路径

1. `/app/tasks` 创建 1 小时内到期任务 → Rail 铃铛出现 `TASK_DUE`
2. 点击通知 → 跳转 `/app/tasks?taskId=...`
3. 双会话：管理端邀请 → 被邀请人在线角标 WS 刷新（可选）
