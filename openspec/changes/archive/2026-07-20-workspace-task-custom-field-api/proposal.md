## Why

母 change P8 / D12：清单自定义单选字段需持久化（定义 + 选项 + EAV 取值），并扩展 `group-move` 支持 `custom:{fieldId}`，供 `-integrate` 去 Mock。

## What Changes

- Flyway `V0.1.0.33`：`task_list_field` / `task_list_field_option` / `task_item_field_value`
- `/app-api/task/list-field/*` REST（对齐 contract）
- `PUT …/item/group-move` 支持自定义字段 + `listId`
- 错误码 `TASK_LIST_FIELD_*`；看板 api ready
- **不改** web Mock（`-integrate` 再去）

## Capabilities

### Modified Capabilities

- `task`：清单自定义单选字段持久化与 group-move 扩展

## Impact

Flyway、`relayflow-module-task-*`、contract、看板
