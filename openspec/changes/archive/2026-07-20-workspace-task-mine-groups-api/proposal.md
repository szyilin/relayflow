## Why

母 change §7.2：交付「我负责的」个人组表与 REST，供 integrate 替换 `USE_LOCAL_MINE_GROUPS`。

## What Changes

- Flyway `task_mine_group` / `task_mine_group_item`
- `/app-api/task/mine-group/*`（list/create/update/delete/move）
- 每用户默认组；删非默认组成员回默认；move 校验任务可读
- 冻结 contract；看板 api=ready
- **不改** `web/`（integrate 另做）

## Capabilities

### Modified Capabilities

- `task`：个人自定义分组持久化与 API

## Impact

`relayflow-server` Flyway、`relayflow-module-task-*`、contract、看板
