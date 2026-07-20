## Why

母 change 要求每个视图上下文持久化 **ViewConfig**（筛选/排序/分组/字段/列表|看板）。需前端先行定稿工具栏与 contract，API 未就绪时用 store 本地临时持久化。

## What Changes

- `/app/tasks` 工具栏：排序、分组、筛选、字段显示、列表|看板
- 按上下文（快捷视图 / 清单）读写 ViewConfig；本地临时存储（integrate 后换 API）
- 列表侧客户端应用排序/筛选/字段显隐（分组呈现留给 `group-by-field`）
- 起草 `openspec/lanes/workspace-task-view-config/contract.md`
- **不改** Java

## Capabilities

### Modified Capabilities

- `task`：工作台 ViewConfig 工具栏（前端）

## Impact

`web/`、`openspec/lanes/workspace-task-view-config/`、对接看板
