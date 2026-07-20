## Why

母 change P8：清单侧自定义单选字段可作为工具栏 `groupBy` 分组源；系统字段分组已交付，但清单仍无法定义「优先级 / 阶段」类协作字段。本切片前端先行，冻结 contract 与 Mock UI，后端另开 `-api`。

## What Changes

- 清单上下文：字段定义 UI（单选 CRUD：名称、选项增删改排序）
- 详情 / 列表：展示并编辑当前清单下的单选取值；空值 →「无分组」
- 工具栏 `groupBy`：可选 `custom:{fieldId}`；列表分区与看板列由选项驱动；跨桶拖拽 Mock
- 起草 `openspec/lanes/workspace-task-custom-field/contract.md`（对齐母 change D12 EAV）
- Store 临时数据 + `USE_LOCAL_CUSTOM_FIELD`（或等价）直至 `-integrate`
- **不改** Java / Flyway

## Capabilities

### New Capabilities

（无——行为仍归 `task` 域）

### Modified Capabilities

- `task`：清单自定义单选字段 UI；`groupBy` 可指向自定义字段；详情取值编辑

## Impact

| 层 | 路径 | 变更 |
|----|------|------|
| 前端 | `web/` `/app/tasks` | 清单字段管理、groupBy 扩展、详情字段 |
| 契约 | `openspec/lanes/workspace-task-custom-field/` | 新建 contract |
| 文档 | `api-integration-board.md` | 登记 `custom-field` → ui_ready |
| 后端 | — | 本切片不做 |
| 回滚 | 关 UI / 删 Mock；无表变更 | |
