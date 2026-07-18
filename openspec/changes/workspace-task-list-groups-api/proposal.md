## Why

母 change §9.2：`task_list_group` + `task_list_item.group_id`；REST 供 list-groups integrate。

## What Changes

- Flyway `V0.1.0.32`；每清单默认组回填
- `/app-api/task/list-group/*`
- 加入清单时写入默认 `group_id`
- contract api ready；**不改** web Mock

## Capabilities

### Modified Capabilities

- `task`：清单内组持久化

## Impact

Flyway、`relayflow-module-task-*`、contract、看板
