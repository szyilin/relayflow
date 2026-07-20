## Why

`workspace-task-custom-field-api` 已交付 EAV REST；前端仍 `USE_LOCAL_CUSTOM_FIELD`。本切片去 Mock、改走 API 并验收。

## What Changes

- `web/src/api/app/taskListField.ts` + store 改走 REST
- 删除 `USE_LOCAL_CUSTOM_FIELD` / 会话种子
- `groupMoveTask` 传 `listId` + `custom:{id}`
- contract → done；看板 done
- **不改** Java（除非联调发现合同缺口）

## Capabilities

### Modified Capabilities

- `task`：清单自定义字段前端接 API

## Impact

`web/`、contract、看板、母 change §10.3
