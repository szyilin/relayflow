## 1. Contract

- [x] 1.1 更新 `openspec/lanes/workspace-task-view-config/contract.md`：FilterClause 字段/运算符；种子写入 filters
- [x] 1.2 更新 `openspec/lanes/workspace-task-quick-views/contract.md`：种子与可见可编辑 filters 对齐

## 2. Model + apply

- [x] 2.1 扩展 `TaskViewFilterClause` 与 `defaultViewConfig` / 种子助手（含 `__ME__`）
- [x] 2.2 `applyClientTransforms` 实现多字段 AND 过滤；快捷入口拉取改 `scope=ALL`（FOLLOWING/LIST 除外）

## 3. UI

- [x] 3.1 `TaskViewToolbar` 飞书式筛选：字段/运算符/值、添加/删除/清空；计数含全部 clause
- [x] 3.2 「重置」恢复入口种子；人员值支持「我」

## 4. Verify

- [x] 4.1 `cd web && pnpm build && pnpm typecheck`
- [x] 4.2 `openspec validate workspace-task-advanced-filters-web --strict`
