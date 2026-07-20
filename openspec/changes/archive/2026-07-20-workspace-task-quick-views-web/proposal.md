## Why

母 change `workspace-task-view-model-v1` 要求左栏「快速访问」全家桶（全部任务 / 我创建的 / 我分配的 / 已完成）作为**预设查询快捷方式**，与清单容器区分。当前 `/app/tasks` 仅有扁平入口且缺「全部任务」「我分配的」，需前端先行定稿导航与默认筛选心智。

## What Changes

- 左栏重组：个人入口（我负责的 / 我关注的 / 动态）→ **快速访问** → 清单
- 新增导航上下文：`all`、`assigned_by_me`；「已完成」「我创建的」归入快速访问
- Store：按 context 套用默认筛选种子；`ALL` / `ASSIGNED_BY_ME` 在 API 未就绪时用 **store 内临时数据**（integrate 删除）
- 起草 `openspec/lanes/workspace-task-quick-views/contract.md`
- **不改** Java / Flyway

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `task`：工作台左栏快速访问预设视图（前端行为；API 形状见 contract）

## Impact

| 层 | 路径 |
|----|------|
| 前端 | `web/src/pages/app/tasks`、`stores/tasks`、`api/app/task` 类型增量 |
| 契约 | `openspec/lanes/workspace-task-quick-views/contract.md` |
| 看板 | `docs/dev/api-integration-board.md` |
| 后端 | 本 change **不改**；`-api` 另开 |
