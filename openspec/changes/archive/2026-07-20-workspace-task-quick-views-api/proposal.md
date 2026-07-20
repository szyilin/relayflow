## Why

`workspace-task-quick-views-web` 已定稿左栏快速访问与 contract；需后端实现 `scope=ALL` / `ASSIGNED_BY_ME`，并落库 **分配人** 以支撑「我分配的」。

## What Changes

- Flyway：`task_item.assigner_id`
- `GET /app-api/task/item/page`：`scope=ALL`（可见并集）、`scope=ASSIGNED_BY_ME`
- 指派写入 `assigner_id`；RespVO 返回 `assignerId`
- 更新 contract 状态；**不改** `web/`（integrate 另开）

## Capabilities

### Modified Capabilities

- `task`：快速访问 page scope；分配人字段

## Impact

| 层 | 路径 |
|----|------|
| DB | `V0.1.0.27__task_item_assigner.sql` |
| 后端 | `relayflow-module-task-biz` |
| 契约 | `openspec/lanes/workspace-task-quick-views/contract.md` |
