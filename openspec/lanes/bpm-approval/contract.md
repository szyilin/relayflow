# API 契约：bpm-approval

> **状态**：草案（母 change 规划；`bpm-approval-web` lane 实施时细化）  
> **起草**：`bpm-v1` change  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **前置**：统一登录、多租户 JWT；通知类型 `APPROVAL_PENDING` 见 [`notify-inbox-v2/contract.md`](../notify-inbox-v2/contract.md)

## 背景

员工工作台 `/app/approvals`：V1 单模板「通用审批」— 提交 → 单人审批 → 通过/驳回。引擎为嵌入式 Flowable，业务扩展表 `bpm_process_instance_ext`。

## 鉴权

| 项 | 值 |
|----|-----|
| REST | `Authorization: Bearer <JWT>` |
| 产品面 | 有效组织成员；**不用** `sys_permission` |

## REST 端点

前缀：`/app-api/bpm`

### POST /instance/submit

**Body**：

```json
{
  "title": "请假申请",
  "summary": "7月15日年假一天",
  "approverId": 1002
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| `title` | 是 | 1–200 字符 |
| `summary` | 否 | 最长 500 |
| `approverId` | 否 | 缺省时取租户默认审批人（首个 super_admin）；无默认则 `BPM_APPROVER_REQUIRED` |

**Response `data`**：

```json
{
  "id": 5001,
  "processInstanceId": "proc-uuid",
  "status": "RUNNING",
  "title": "请假申请",
  "applicantId": 1001,
  "approverId": 1002,
  "createTime": "2026-07-13T18:00:00+08:00"
}
```

### GET /instance/mine/page

**Query**：`pageNo`、`pageSize`（标准分页）

**Response `data`**：`list[]` 含 `id`、`title`、`summary`、`status`（`RUNNING` \| `APPROVED` \| `REJECTED` \| `CANCELLED`）、`approverId`、`createTime`、`updateTime`

### GET /task/todo/page

**Query**：`pageNo`、`pageSize`

**Response `data`**：`list[]` 含 `taskId`、`instanceId`（ext id）、`title`、`summary`、`applicantId`、`applicantNickname`、`createTime`

### GET /instance/get

**Query**：`id`（ext 表主键）

**Response `data`**：实例详情 + 当前待办 `taskId`（若 `RUNNING` 且当前用户为审批人）

### POST /task/approve

**Body**：

```json
{ "taskId": "flowable-task-id", "comment": "同意" }
```

**Response `data`**：`true`

### POST /task/reject

**Body**：

```json
{ "taskId": "flowable-task-id", "comment": "请补充说明" }
```

**Response `data`**：`true`

## 错误码

| code | 说明 |
|------|------|
| `BPM_INSTANCE_NOT_FOUND` | 实例不存在或非本租户 |
| `BPM_TASK_NOT_FOUND` | 任务不存在或已处理 |
| `BPM_TASK_FORBIDDEN` | 非任务 assignee |
| `BPM_APPROVER_REQUIRED` | 未指定审批人且无默认 |

## 通知（可选联调）

待办创建后 `bpm-biz` 调用 `NotifyInboxApi.push`：

```json
{
  "type": "APPROVAL_PENDING",
  "dedupeKey": "approval:{processInstanceId}",
  "title": "待你审批",
  "body": "「请假申请」待处理",
  "payload": {
    "route": "/app/approvals?instanceId=5001",
    "entityType": "approval",
    "entityId": "5001"
  }
}
```

依赖 `notify-inbox-v2` §1 类型目录；未上线时待办列表仍须可用。

## 前端 UI

| 区域 | 说明 |
|------|------|
| Rail | 新增「审批」`id=approvals`，`i-lucide-file-check`，`/app/approvals` |
| Panel | Tab：待我审批 / 我发起的 |
| Main | 列表 + `UDrawer` 详情；审批人可见通过/驳回 |
| 新建 | `UModal`：标题、说明、`USelectMenu` 审批人（通讯录成员 API） |
| 深链 | `?instanceId=` 打开对应详情 |

Store：`stores/approvals.ts` → `api/app/bpm.ts`；页面不 import `mocks/`。

## curl 示例

```bash
# 提交
curl -s -X POST http://localhost:8080/app-api/bpm/instance/submit \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"请假申请","approverId":1002}' | jq

# 待我审批
curl -s 'http://localhost:8080/app-api/bpm/task/todo/page?pageNo=1&pageSize=20' \
  -H "Authorization: Bearer $TOKEN" | jq

# 通过
curl -s -X POST http://localhost:8080/app-api/bpm/task/approve \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"taskId":"..."}' | jq
```

## 浏览器验证路径

1. 用户 A `/app/approvals` → 新建审批，指定 B 为审批人
2. 用户 B 登录 → 待我审批可见 → 打开详情 → 通过
3. 用户 A「我发起的」状态为 `APPROVED`
4. （可选）B 铃铛出现 `APPROVAL_PENDING`，点击跳转 `/app/approvals?instanceId=...`
