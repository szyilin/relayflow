## Why

ViewConfig 已可保存 `groupBy`，但列表/看板仍按写死状态三列或扁平列表呈现。需前端先行：按系统字段分组分区、「无分组」、拖拽改字段（Mock）。

## What Changes

- 列表按 `groupBy` 分区；看板列由分组桶驱动
- 空值桶「无分组」
- 拖拽跨桶本地改字段（`USE_LOCAL_GROUP_MOVE`，integrate 后接 API）
- `PERSONAL_CUSTOM` / `LIST_GROUP` 本切片仅占位提示
- 起草 `openspec/lanes/workspace-task-group-by-field/contract.md`
- **不改** Java

## Capabilities

### Modified Capabilities

- `task`：按字段分组呈现（前端）

## Impact

`web/`、contract、看板
