## Why

`workspace-task-multi-assignee-web` 已定稿多选 UI 与 contract；需后端 `task_item_assignee` + `PUT /assignees`，MINE=集合包含我。

## What Changes

- Flyway `task_item_assignee` + 从 `assignee_id` 回填
- `PUT /app-api/task/item/assignees`；`PUT /assign` 兼容为单元素替换
- page/search/due-range/可见 union 改读集合；响应填 `assigneeIds`
- due Bot / assign Bot 按负责人 fan-out（best-effort）
- **不改** `web/`

## Capabilities

### Modified Capabilities

- `task`：多负责人持久化与查询

## Impact

`relayflow-module-task-*`、Flyway、contract、看板
