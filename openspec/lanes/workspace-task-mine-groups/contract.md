# API 契约：workspace-task-mine-groups

> **状态**：draft（`-web` 起草；`-api` 未实现）  
> **起草**：`workspace-task-mine-groups-web`  
> **母 change**：[`workspace-task-view-model-v1`](../../changes/workspace-task-view-model-v1/proposal.md)  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **相关**：[`workspace-task-view-config`](../workspace-task-view-config/contract.md)

## 背景

「我负责的」个人逻辑分组（计划 B）：仅本人可见；与工具栏字段分组独立。仅当 `groupBy.mode=PERSONAL_CUSTOM` 时驱动分区。

## 鉴权

仅当前登录用户读写自己的组与归属。禁止访问他人 `user_id` 下的组。

## 数据模型（`-api`）

### `task_mine_group`

| 列 | 说明 |
|----|------|
| `id` | 雪花 |
| `tenant_id` | 租户 |
| `user_id` | 所有者 |
| `name` | 组名 |
| `rank` | 排序 |
| `is_default` | 每用户恰 1 个 true |

### `task_mine_group_item`

| 列 | 说明 |
|----|------|
| `tenant_id` | 租户 |
| `user_id` | 所有者 |
| `task_id` | 任务 |
| `group_id` | 个人组 |
| `rank` | 组内序（可选） |
| 唯一 | `(tenant_id, user_id, task_id)` |

入 MINE 且无归属 → 写入默认组。

## REST（草案 · `-api`）

前缀：`/app-api/task/mine-group`

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/list` | 当前用户组列表（含默认） |
| `POST` | `/create` | `{ name }` |
| `PUT` | `/update` | `{ id, name?, rank? }`（不可改 is_default） |
| `DELETE` | `/delete` | `?id=` 非默认；成员回默认组 |
| `PUT` | `/move` | `{ taskId, groupId, beforeId? }` |

## `-web` 临时行为

`USE_LOCAL_MINE_GROUPS`：Pinia 内存；刷新丢失；integrate 删除。

## 错误码

| code | 说明 |
|------|------|
| `TASK_MINE_GROUP_NOT_FOUND` | 组不存在或不属于我 |
| `TASK_MINE_GROUP_FORBIDDEN` | 删默认组等非法操作 |
| `TASK_NOT_FOUND` / `TASK_FORBIDDEN` | 任务无权 |
