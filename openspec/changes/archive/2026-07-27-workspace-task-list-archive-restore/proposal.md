## Why

清单「归档」已是软隐藏（`archived=1`，不删任务），但侧栏只展示未归档清单，且无已归档列表与恢复入口，用户体感等同删除。需要补齐「已归档列表 + 恢复」，明确归档 ≠ 删除。

## What Changes

- 扩展 `workspace-task-list` 契约：`GET /list/archived`、`POST /list/unarchive`
- 后端实现上述 API（`relayflow-module-task-biz`）
- `/app/tasks` 侧栏增加「已归档」分区：查看、恢复；归档前确认文案说明可恢复
- 更新对接看板；无 Flyway（沿用既有 `archived` 列）

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `task`: 清单归档可发现、可恢复；归档仍不删任务

## Impact

- 模块：`relayflow-module-task`（biz Controller/Service）、`web/`（tasks 侧栏 + store/api）
- 契约：`openspec/lanes/workspace-task-list/contract.md`
- 回滚：仅代码回退；无迁移；已归档数据保持 `archived=1`
