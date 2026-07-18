# API 契约：workspace-task-multi-list

> **状态**：draft（`-web` 起草；`-api` 未实现）  
> **起草**：`workspace-task-multi-list-web`  
> **母 change**：[`workspace-task-view-model-v1`](../../changes/workspace-task-view-model-v1/proposal.md)  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **相关**：[`workspace-task-list`](../workspace-task-list/contract.md)

## 背景

任务可属于 0..N 个清单（D6 **BREAKING**）。`task_list_item` 替换单列 `task_item.list_id` 作为真源。移出清单 **不** 删除任务。清单内组 `group_id` 由 P7 扩展。

## 鉴权

对每个目标清单须有可变更任务权限（OWNER/EDITOR，或既有 `requireCanMutateTasks` 规则）。读详情须能访问任务。

## 数据模型（`-api`）

### `task_list_item`

| 列 | 说明 |
|----|------|
| `id` | 雪花 |
| `tenant_id` | 租户 |
| `list_id` | 清单 |
| `task_id` | 根任务 |
| `group_id` | 可选；P7 清单内组 |
| `rank` | 可选排序 |
| 唯一 | `(tenant_id, list_id, task_id)` WHERE `deleted=0` |

迁移：非空 `task_item.list_id` → 插入一行；之后停写 `list_id`（可读兼容投影）。

## TaskItem 响应增量

| 字段 | 说明 |
|------|------|
| `listIds` | 所属清单 id 列表 |
| `listId` | 兼容：首个或 null |
| `listMemberships` | 可选：`[{ listId, listName, groupId? }]` |

## REST（草案 · `-api`）

| 方法 | 路径 | 说明 |
|------|------|------|
| `PUT` | `/app-api/task/item/list-memberships` | `{ id, listIds: long[] }` 全量替换 |

`GET …/page?listId=` → 按 `task_list_item` 过滤。

## `-web` 临时行为

`USE_LOCAL_MULTI_LIST`：仅改 Pinia 缓存；刷新丢失；integrate 删除。

## 错误码

| code | 说明 |
|------|------|
| `TASK_LIST_NOT_FOUND` | 清单不存在 |
| `TASK_LIST_FORBIDDEN` | 无权加入/移出该清单 |
| `TASK_NOT_FOUND` / `TASK_FORBIDDEN` | 任务无权 |
