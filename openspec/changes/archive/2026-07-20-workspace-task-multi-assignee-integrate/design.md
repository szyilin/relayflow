## Context

接 multi-assignee-web/api。

## Goals / Non-Goals

**Goals:** 去 Mock；详情保存落库；移出自己后「我负责的」刷新消失。

**Non-Goals:** 删 `PUT /assign`；改 Bot。

## Decisions

1. `setAssignees` → `PUT /assignees`；乐观更新 + 失败回滚，成功后按需 `fetchMyTasks` / `selectTask`。
2. `assignTo` 统一委托 `setAssignees([id])`。
3. 删除 `USE_LOCAL_MULTI_ASSIGNEE`。
