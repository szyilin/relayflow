## Why

「我分配的」入口与 `assignerId` 已随 quick-views 落地；详情仍不展示分配人，用户难理解「我分配的」来源。本切片补 UI 与 contract 收口。

## What Changes

- 详情展示「分配人」（有 `assignerId` 时）
- 「我分配的」列表可感知分配语义文案
- 起草 `openspec/lanes/workspace-task-assigner/contract.md`（对齐既有写入规则）
- **不改** Java（持久化与 ASSIGNED_BY_ME 查询已在 quick-views/multi-assignee）

## Capabilities

### Modified Capabilities

- `task`：分配人展示（前端）

## Impact

`web/`、contract、看板
