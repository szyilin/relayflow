# API 契约：workspace-task-list-groups

> **状态**：draft（`-web` 起草；`-api` 未实现）  
> **起草**：`workspace-task-list-groups-web`  
> **母 change**：[`workspace-task-view-model-v1`](../../changes/workspace-task-view-model-v1/proposal.md)  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **相关**：[`workspace-task-multi-list`](../workspace-task-multi-list/contract.md)、[`workspace-task-view-config`](../workspace-task-view-config/contract.md)

## 背景

清单内逻辑分组（计划 C / D7）：清单成员可见；写在 `task_list_item.group_id`。仅当查看清单且 `groupBy` 非 FIELD（`null` 或 `LIST_GROUP`）时用 C 驱动分区；FIELD 优先。

## 鉴权

须为该清单成员；建删组 / 移动任务须 OWNER|EDITOR（`requireCanMutateTasks`）。

## 数据模型（`-api`）

### `task_list_group`

| 列 | 说明 |
|----|------|
| `id` | 雪花 |
| `tenant_id` | 租户 |
| `list_id` | 清单 |
| `name` | 组名 |
| `rank` | 排序 |
| `is_default` | 每清单恰 1 个 true |

### `task_list_item.group_id`

指向 `task_list_group.id`；空则视为默认组（或写入时 ensure 默认）。

## REST（草案 · `-api`）

前缀：`/app-api/task/list-group`

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/list?listId=` | 组列表 + 该清单下任务的 group 归属 |
| `POST` | `/create` | `{ listId, name }` |
| `PUT` | `/update` | `{ id, name?, rank? }` |
| `DELETE` | `/delete` | `?id=` 非默认；成员回默认组 |
| `PUT` | `/move` | `{ listId, taskId, groupId, beforeId? }` |

## `-web` 临时行为

`USE_LOCAL_LIST_GROUPS`：按 listId 内存隔离；刷新丢失；integrate 删除。

## 错误码

| code | 说明 |
|------|------|
| `TASK_LIST_GROUP_NOT_FOUND` | 组不存在或不属于该清单 |
| `TASK_LIST_GROUP_FORBIDDEN` | 删默认组等 |
| `TASK_LIST_FORBIDDEN` | 无权 |
| `TASK_NOT_FOUND` / `TASK_FORBIDDEN` | 任务无权 |
