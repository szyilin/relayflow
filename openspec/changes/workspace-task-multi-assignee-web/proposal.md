## Why

母 change P3：详情仍单负责人指派；需前端先行多选负责人 UI，「我负责的」= 负责人集合包含我。

## What Changes

- `TaskItem.assigneeIds`（兼容 `assigneeId` 投影）
- 详情负责人多选 / 增删；本地 `USE_LOCAL_MULTI_ASSIGNEE` 直至 integrate
- 起草 `openspec/lanes/workspace-task-multi-assignee/contract.md`
- **不改** Java

## Capabilities

### Modified Capabilities

- `task`：多负责人 UI（前端）

## Impact

`web/`、contract、看板
