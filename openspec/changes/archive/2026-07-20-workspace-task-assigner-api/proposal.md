## Why

分配人持久化与 `ASSIGNED_BY_ME` 查询已在 quick-views / multi-assignee 落地；本切片正式收口 contract、补单测对齐 D8，不新增表。

## What Changes

- 确认 `TaskAssigneeService.syncProjection` 与 `scope=ASSIGNED_BY_ME` 语义
- 单测覆盖：派给他人写 assigner；含自己则清 assigner
- 冻结 `workspace-task-assigner` contract api ready；**不改** `web/`

## Capabilities

### Modified Capabilities

- `task`：分配人 API 收口

## Impact

`relayflow-module-task-biz` 测试、contract、看板
