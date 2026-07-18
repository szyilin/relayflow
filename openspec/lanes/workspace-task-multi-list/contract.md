# API 契约：workspace-task-multi-list

> **状态**：done（integrate 完成）  
> **起草**：`workspace-task-multi-list-web`  
> **实现**：`workspace-task-multi-list-api`  
> **联调**：`workspace-task-multi-list-integrate`  
> **母 change**：[`workspace-task-view-model-v1`](../../changes/workspace-task-view-model-v1/proposal.md)  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **相关**：[`workspace-task-list`](../workspace-task-list/contract.md)

## 背景

任务可属于 0..N 个清单（D6 **BREAKING**）。`task_list_item` 替换单列 `task_item.list_id` 作为真源。移出清单 **不** 删除任务。清单内组 `group_id` 由 P7 扩展。

## 鉴权

- 读详情：须能访问任务。
- `PUT /list-memberships`：须 `requireEditable`；**新增**的每个清单须 `requireCanMutateTasks`（OWNER/EDITOR）。

## 数据模型

### `task_list_item`（Flyway `V0.1.0.31`）

| 列 | 说明 |
|----|------|
| `id` | 雪花 |
| `tenant_id` | 租户 |
| `list_id` | 清单 |
| `task_id` | 根任务 |
| `group_id` | 可选；P7 |
| `rank` | 排序 |
| 唯一 | `(tenant_id, list_id, task_id)` WHERE `deleted=0` |

迁移：非空根任务 `list_id` → 插入一行。兼容投影：`task_item.list_id` = `listIds` 首个（或 null）。

## TaskItem 响应增量

| 字段 | 说明 |
|------|------|
| `listIds` | 所属清单 id 列表 |
| `listId` | 兼容：首个或 null |

## REST

| 方法 | 路径 | 说明 |
|------|------|------|
| `PUT` | `/app-api/task/item/list-memberships` | `{ id, listIds: long[] }` 全量替换 |

`GET …/page?listId=` → `EXISTS task_list_item`。创建根任务带 `listId` → 写入成员表。子任务复制父成员集。

## 前端

Store `setListMemberships` 走真 API；无本地 Mock 标志。

## 错误码

| code | 说明 |
|------|------|
| `TASK_LIST_NOT_FOUND` | 清单不存在 |
| `TASK_LIST_FORBIDDEN` | 无权加入该清单 |
| `TASK_NOT_FOUND` / `TASK_FORBIDDEN` | 任务无权 |
