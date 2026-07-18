# API 契约：workspace-task-multi-assignee

> **状态**：api ready（`-api` 已实现；`-integrate` 待接前端）  
> **起草**：`workspace-task-multi-assignee-web`  
> **实现**：`workspace-task-multi-assignee-api`  
> **母 change**：[`workspace-task-view-model-v1`](../../changes/workspace-task-view-model-v1/proposal.md)  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **相关**：[`workspace-task-quick-views`](../workspace-task-quick-views/contract.md)、[`workspace-task-detail`](../workspace-task-detail/contract.md)

## 背景

任务负责人从单列 `assignee_id` 升级为多选集合。`「我负责的」` = 负责人集合 **包含** 当前用户。写路径真源：`task_item_assignee`。

## 鉴权

与现有 assign / 任务写一致：`requireEditable`（creator / 集合内 / list EDITOR+）。集合中每位用户须为本租户有效成员。

## 数据模型

### `task_item_assignee`（Flyway `V0.1.0.29`）

| 列 | 说明 |
|----|------|
| `id` | 雪花 |
| `tenant_id` | 租户 |
| `task_id` | 任务 |
| `user_id` | 负责人 |
| 唯一 | `(tenant_id, task_id, user_id)` WHERE deleted=0 |

迁移：非空 `assignee_id` 回填一行；写集合时同步投影 `task_item.assignee_id` = 集合最小 `user_id`（空 → null）。

### TaskItem 响应增量

```json
{
  "assigneeId": "100",
  "assigneeIds": ["100", "200"]
}
```

| 字段 | 说明 |
|------|------|
| `assigneeIds` | 负责人 id 列表（升序；可空） |
| `assigneeId` | 投影：首个负责人或 null |

## REST

前缀：`/app-api/task/item`

| 方法 | 路径 | 说明 |
|------|------|------|
| `PUT` | `/assignees` | Body `{ id, assigneeIds: long[] }` 全量替换 |
| `PUT` | `/assign` | 兼容：等价 `assigneeIds: [assigneeId]` |

### PUT /assignees

```json
{
  "id": "1001",
  "assigneeIds": ["100", "200"]
}
```

- 空数组：清除全部负责人（允许）
- 非法成员 → `TASK_ASSIGNEE_NOT_MEMBER`
- 新增成员：best-effort `task-bot`
- assigner：集合不含自己且含他人 → `assignerId=当前用户`；含自己或空 → `assignerId=null`

## 「我负责的」查询

默认 page：根任务且 `EXISTS` 当前用户在 `task_item_assignee`。search / due-range / ALL union / access 同读集合。

## due Bot

每负责人一条：`dedupeKey=TASK_DUE:{taskId}:{userId}`。

## `-web` 临时行为（integrate 删除）

`USE_LOCAL_MULTI_ASSIGNEE`：仅改 Pinia；integrate 后改走本 API。

## 错误码

| code | 说明 |
|------|------|
| `TASK_FORBIDDEN` | 无权 |
| `TASK_NOT_FOUND` | 不存在 |
| `TASK_ASSIGNEE_NOT_MEMBER` | 集合含非有效成员 |

## curl

```bash
curl -s -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"id":"1001","assigneeIds":[100,200]}' \
  "$BASE/app-api/task/item/assignees"
```
