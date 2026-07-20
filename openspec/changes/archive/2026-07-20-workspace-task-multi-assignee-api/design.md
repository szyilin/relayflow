## Context

接 multi-assignee-web；母 change D2/D9 / §5.2。

## Goals / Non-Goals

**Goals:** 表 + 写路径真源；MINE/search/due/access 读集合；响应 `assigneeIds`；`assignee_id` 投影同步。

**Non-Goals:** 改前端；删 `assignee_id` 列；日历 UI。

## Decisions

1. `task_item_assignee` 唯一 `(tenant_id, task_id, user_id)`（软删 partial unique）。
2. 写集合时同步 `task_item.assignee_id` = 最小 `user_id`（稳定投影）；空集合 → null。
3. `PUT /assign` 委托 `replaceAssignees([id])`。
4. due Bot：`dedupeKey=TASK_DUE:{taskId}:{userId}` 每负责人一条。
5. access/edit：creator / 集合内 / list EDITOR+。

## Risks

双写投影不一致 → 仅通过 `TaskAssigneeService.replaceAssignees` 写入。
