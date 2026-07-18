# API 契约：workspace-task-mine-groups

> **状态**：api ready（`-api` 已实现；待 integrate）  
> **起草**：`workspace-task-mine-groups-web`  
> **实现**：`workspace-task-mine-groups-api`  
> **母 change**：[`workspace-task-view-model-v1`](../../changes/workspace-task-view-model-v1/proposal.md)  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **相关**：[`workspace-task-view-config`](../workspace-task-view-config/contract.md)

## 背景

「我负责的」个人逻辑分组（计划 B）：仅本人可见；与工具栏字段分组独立。仅当 `groupBy.mode=PERSONAL_CUSTOM` 时驱动分区。

## 鉴权

仅当前登录用户读写自己的组与归属。禁止访问他人 `user_id` 下的组。`PUT /move` 须能访问目标任务（`TaskAccessService.requireAccessible`）。

## 数据模型

### `task_mine_group`（Flyway `V0.1.0.30`）

| 列 | 说明 |
|----|------|
| `id` | 雪花 |
| `tenant_id` | 租户 |
| `user_id` | 所有者 |
| `name` | 组名 |
| `rank` | 排序 |
| `is_default` | `SMALLINT`：每用户恰 1 个为 `1` |

### `task_mine_group_item`

| 列 | 说明 |
|----|------|
| `tenant_id` | 租户 |
| `user_id` | 所有者 |
| `task_id` | 任务 |
| `group_id` | 个人组 |
| `rank` | 组内序 |
| 唯一 | `(tenant_id, user_id, task_id)` WHERE `deleted=0` |

入 MINE（创建根任务默认把自己设为负责人）且无归属 → 写入默认组。`GET /list` 会 ensure 默认组。

## REST

前缀：`/app-api/task/mine-group`

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/list` | `{ groups, memberships }`；无默认组则创建 |
| `POST` | `/create` | `{ name }` → 组 VO |
| `PUT` | `/update` | `{ id, name?, rank? }`（不可改 `isDefault`） |
| `DELETE` | `/delete` | `?id=` 非默认；成员回默认组 |
| `PUT` | `/move` | `{ taskId, groupId, beforeId? }` |

### `GET /list` 响应

```json
{
  "groups": [{ "id": 1, "name": "默认", "rank": 0, "isDefault": true }],
  "memberships": [{ "taskId": 100, "groupId": 1, "rank": 0 }]
}
```

未出现在 `memberships` 中的任务：前端可视为默认组（或在展示前调用 list 后由 ensure 补齐；创建路径已写入）。

## `-web` 临时行为

`USE_LOCAL_MINE_GROUPS`：Pinia 内存；**integrate 删除并改走本契约**。

## 错误码

| code | 说明 |
|------|------|
| `TASK_MINE_GROUP_NOT_FOUND` | 组不存在或不属于我 |
| `TASK_MINE_GROUP_FORBIDDEN` | 删默认组等非法操作 |
| `TASK_MINE_GROUP_NAME_EMPTY` | 名称为空 |
| `TASK_NOT_FOUND` / `TASK_FORBIDDEN` | 任务无权 |
