## Why

当前 `/app/tasks` 筛选工具栏仅支持状态勾选，且快捷访问的默认条件藏在 API `scope` 里、筛选面板不可见。用户无法理解「我创建的 / 已完成」等入口如何工作，也无法像飞书一样用「字段 + 运算符 + 值」组合条件。

## What Changes

- 筛选面板升级为可编辑的多条件规则（字段 / 运算符 / 值，AND 组合），对齐飞书心智
- 快捷访问入口的默认筛选种子写入 ViewConfig.filters 并在面板中展示；用户可改、可删、可追加
- 用户改动后仍停留在该入口，查询按已保存的 filters 生效；「重置」恢复该入口默认种子
- 客户端对已加载列表应用完整 filters（本切片）；服务端按 clause 过滤留给后续 `-api`
- 更新 view-config / quick-views contract 中 filters 形状与种子说明

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `task`: 视图筛选从「仅 status 叠加」升级为可见可编辑的多字段 FilterClause；快捷访问种子必须出现在 filters 中

## Impact

- **前端** `web/`：`TaskViewToolbar`、`viewConfigLocal` / `viewConfigStore`、任务列表拉取策略（个人快捷入口以可见并集为底，客户端按 filters 收窄）
- **契约** `openspec/lanes/workspace-task-view-config/contract.md`、`workspace-task-quick-views/contract.md`
- **后端**：本切片不改 Java；ViewConfig JSON 已为宽松 Map 列表，可透传新 clause。完整服务端过滤另开 `-api`
- **回滚**：前端回退即可；已持久化的新 shape filters 旧 UI 会忽略未知字段
