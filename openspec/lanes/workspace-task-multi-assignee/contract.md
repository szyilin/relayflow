# API 契约：workspace-task-multi-assignee

> **状态**：draft（`-web` 起草；`-api` 未实现）  
> **起草**：`workspace-task-multi-assignee-web`  
> **母 change**：[`workspace-task-view-model-v1`](../../changes/workspace-task-view-model-v1/proposal.md)  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **相关**：[`workspace-task-quick-views`](../workspace-task-quick-views/contract.md)、[`workspace-task-detail`](../workspace-task-detail/contract.md)

## 背景

任务负责人从单列 `assignee_id` 升级为多选集合。`「我负责的」` = 负责人集合 **包含** 当前用户。写路径真源：`task_item_assignee`（`-api`）。

## 鉴权

与现有 assign / 任务写一致：`requireEditable`。集合中每位用户须为本租户有效成员。

## 数据模型（`-api`）

### `task_item_assignee`

| 列 | 说明 |
|----|------|
| `id` | 雪花 |
| `tenant_id` | 租户 |
| `task_id` | 任务 |
| `user_id` | 负责人 |
| 唯一 | `(tenant_id, task_id, user_id)` |

迁移：把现有非空 `assignee_id` 灌入一行；响应保留 `assigneeId` = 集合首元素（稳定排序：`user_id` 升序或加入序）以兼容旧客户端。

### TaskItem 响应增量

```json
{
  "assigneeId": "100",
  "assigneeIds": ["100", "200"]
}
```

| 字段 | 说明 |
|------|------|
| `assigneeIds` | 负责人 id 列表（可空数组） |
| `assigneeId` | 投影：首个负责人或 null（兼容分组/旧 UI） |

## REST（草案 · `-api`）

前缀：`/app-api/task/item`

| 方法 | 路径 | 说明 |
|------|------|------|
| `PUT` | `/assignees` | Body `{ id, assigneeIds: string[] }` 全量替换集合 |

### PUT /assignees

```json
{
  "id": "1001",
  "assigneeIds": ["100", "200"]
}
```

- 空数组：清除全部负责人（允许）
- 非法成员 → `TASK_ASSIGNEE_NOT_MEMBER`
- 相对旧集合新增的成员：best-effort `task-bot`（与现 assign 一致，`-api`）
- `assigner` 规则：若操作后集合不含自己且含他人 → `assignerId=当前用户`；若含自己 → 可清 `assignerId`（与现 assign 对齐，细规见 assigner 切片）

**兼容**：`PUT /assign` `{ id, assigneeId }` 视为 `assigneeIds: [assigneeId]` 的过渡写法。

## 「我负责的」查询

默认 page（无特殊 scope）/`scope` 缺省：根任务且 `EXISTS` 当前用户在 `task_item_assignee`。

## `-web` 临时行为

`USE_LOCAL_MULTI_ASSIGNEE`：仅改 Pinia 的 `assigneeIds`/`assigneeId`；mine 列表本地过滤「不含我」；integrate 删除。

## 错误码

| code | 说明 |
|------|------|
| `TASK_FORBIDDEN` | 无权 |
| `TASK_NOT_FOUND` | 不存在 |
| `TASK_ASSIGNEE_NOT_MEMBER` | 集合含非有效成员 |
