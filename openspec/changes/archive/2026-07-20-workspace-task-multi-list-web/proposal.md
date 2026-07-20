## Why

母 change P6：任务详情需多清单加入/移出；当前仅单 `listId`，无详情编辑。

## What Changes

- 详情「所属清单」多选；`listIds` + 本地 Mock（`USE_LOCAL_MULTI_LIST`）
- 起草 `openspec/lanes/workspace-task-multi-list/contract.md`
- **不改** Java（`-api` 另做）

## Capabilities

### Modified Capabilities

- `task`：多清单归属 UI（前端）

## Impact

`web/`、contract、看板
