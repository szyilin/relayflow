# API 契约：workspace-task-group-by-field

> **状态**：draft（`-web` 起草；`-api` 未实现）  
> **起草**：`workspace-task-group-by-field-web`  
> **母 change**：[`workspace-task-view-model-v1`](../../changes/workspace-task-view-model-v1/proposal.md)  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **相关**：[`workspace-task-view-config`](../workspace-task-view-config/contract.md)、[`workspace-task-board`](../workspace-task-board/contract.md)

## 背景

ViewConfig.`groupBy.mode=FIELD` 驱动列表分区与看板列。空值 →「无分组」。跨桶拖拽写回对应系统字段。

## 鉴权

与任务写权限一致（assignee/creator/list EDITOR+|OWNER）。VIEWER 不可拖。

## 分桶规则（V1 系统字段）

| fieldKey | 桶 key | 标签 |
|----------|--------|------|
| `status` | `TODO` / `IN_PROGRESS` / `DONE` | 未开始 / 进行中 / 已完成 |
| `dueTime` | `YYYY-MM-DD` 或 `__empty__` | 日期或「无分组」 |
| `assigneeId` | userId 或 `__empty__` | 用户 id（后续可昵称）或「无分组」 |

稳定空桶 key：`__empty__`，展示名「无分组」。

## REST（草案 · `-api`）

前缀：`/app-api/task/item`

| 方法 | 路径 | 说明 |
|------|------|------|
| `PUT` | `/group-move` | Body `{ id, fieldKey, value }`；`value` 可为 `null`（拖入无分组）；status 不可非法值 |

兼容：清单内 `fieldKey=status` 可继续映射既有 `board-move`（过渡）；新客户端优先 `group-move`。

### group-move Body

```json
{
  "id": "1001",
  "fieldKey": "status",
  "value": "IN_PROGRESS",
  "beforeId": null
}
```

`beforeId`：同桶内排序（可选，后续 rank）。

## `-web` 临时行为

`USE_LOCAL_GROUP_MOVE`：仅改 Pinia 内存中的 task 字段；刷新丢失。integrate 删除。

## 错误码

| code | 说明 |
|------|------|
| `TASK_FORBIDDEN` | 无权改字段 |
| `TASK_NOT_FOUND` | 任务不存在 |
| `TASK_GROUP_MOVE_INVALID` | 字段/值非法（如清空 status） |
