## Why

母 change §8.2：`task_list_item` + `PUT /list-memberships`；page 按成员表过滤。

## What Changes

- Flyway `V0.1.0.31` + 回填 `list_id`
- `PUT /app-api/task/item/list-memberships`
- 响应 `listIds`；创建/子任务写成员表；可见性 union / 访问控制改读成员表
- contract api ready；看板；**不改** web Mock

## Capabilities

### Modified Capabilities

- `task`：多清单持久化与 API

## Impact

Flyway、`relayflow-module-task-*`、contract、看板
