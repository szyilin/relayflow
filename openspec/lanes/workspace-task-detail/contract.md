# API 契约：workspace-task-detail

> **状态**：done（`-integrate` 完成）  
> **起草**：`workspace-task-core-v1` / `workspace-task-detail-web`  
> **实现**：`workspace-task-detail-api` + `workspace-task-detail-integrate`  
> **母 change**：[`openspec/changes/workspace-task-core-v1`](../../changes/workspace-task-core-v1/proposal.md)  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **既有**：[`workspace-tasks/contract.md`](../workspace-tasks/contract.md)（page/create/toggle/delete 仍有效）

## 背景

`/app/tasks` P0：右侧详情面板；扩展起止时间、提醒、描述；一层子任务与进度。

## 鉴权

| 项 | 值 |
|----|-----|
| REST | `Authorization: Bearer <JWT>` |
| 产品面 | 有效组织成员；**不用** `sys_permission` |

## 时间约定

- `startTime` / `dueTime`：ISO-8601 含偏移，可空
- 若两者皆有，必须 `startTime <= dueTime`
- `remindBeforeMinutes`：相对 **dueTime** 的提前分钟数；`null`/缺省 = 沿用服务端 due-window；`0` = 不按显式偏移提醒

## REST（扩展）

前缀：`/app-api/task/item`

### GET /get?id=

**Response `data`**：

```json
{
  "id": "1001",
  "title": "整理周报",
  "status": "TODO",
  "startTime": "2026-07-17T15:00:00+08:00",
  "dueTime": "2026-07-18T18:00:00+08:00",
  "remindBeforeMinutes": 30,
  "description": "附上周数据",
  "parentId": null,
  "assigneeId": "1",
  "creatorId": "1",
  "createTime": "2026-07-17T14:00:00+08:00",
  "subtaskDoneCount": 0,
  "subtaskTotal": 2
}
```

### PUT /update（扩展字段）

```json
{
  "id": "1001",
  "title": "整理周报",
  "startTime": "2026-07-17T15:00:00+08:00",
  "dueTime": "2026-07-18T18:00:00+08:00",
  "remindBeforeMinutes": 30,
  "description": "附上周数据"
}
```

任意字段可选；未传不改。`dueTime: null` 可清空截止。

### GET /page（行为收紧）

默认只返回 **根任务**（`parentId` 为空）。响应元素可带 `subtaskDoneCount` / `subtaskTotal`。

### GET /subtasks?parentId=

返回该父任务下一层子任务列表（同 TaskItem 形状，`parentId` 非空）。

### POST /subtask/create

```json
{ "parentId": "1001", "title": "收集数据" }
```

**Response `data`**：子任务 id。父任务若已有 `parentId` → 业务错误（深度限制 1）。

### PUT /toggle-done / DELETE /delete

可用于根任务与子任务（权限：当前用户为负责人）。

## 错误码（增补）

| code | 说明 |
|------|------|
| `TASK_NOT_FOUND` | 任务不存在 |
| `TASK_FORBIDDEN` | 无权操作 |
| `TASK_INVALID_TIME_RANGE` | start > due |
| `TASK_SUBTASK_DEPTH_EXCEEDED` | 禁止在子任务下再建子任务 |

## 前端行为

| 项 | 约定 |
|----|------|
| 布局 | 左导航 + 中列表 + 右详情；`?taskId=` 选中并打开详情 |
| 看板 Tab | 仍占位 |
| 真源 | 详情/子任务仅走 API；不使用 localStorage 覆盖 |

## curl 示例

```bash
curl -sS -H "Authorization: Bearer $TOKEN" \
  "$BASE/app-api/task/item/get?id=1001"

curl -sS -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"id":"1001","description":"附上周数据","remindBeforeMinutes":30}' \
  "$BASE/app-api/task/item/update"
```
