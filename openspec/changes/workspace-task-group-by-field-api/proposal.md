## Why

`workspace-task-group-by-field-web` 已定稿按字段分桶与拖拽 Mock；需后端 `PUT /group-move` 落库，并说明 `board-move` 过渡兼容。

## What Changes

- `PUT /app-api/task/item/group-move`（status / dueTime / assigneeId；`value=null` → 无分组）
- 错误码 `TASK_GROUP_MOVE_INVALID`
- contract 冻结；看板 api=archived；**不改** `web/`（integrate 另开）
- `board-move` 保留作 status 兼容，文档标注 deprecated 方向

## Capabilities

### Modified Capabilities

- `task`：字段分组拖拽落库 API

## Impact

`relayflow-module-task-*`、contract、看板、母 change §4.2
