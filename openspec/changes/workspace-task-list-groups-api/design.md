## Context

接 list-groups-web；契约见 lane contract。

## Goals / Non-Goals

**Goals:** 表 + ensure 默认组；CRUD/move；加入清单 ensure 默认 group_id。

**Non-Goals:** 前端去 Mock；详情改 C。

## Decisions

1. `is_default` SMALLINT 0/1。
2. `GET /list` 返回 groups + memberships（taskId→groupId，仅该 list）。
3. 新成员行：`group_id` = 该清单默认组。
4. 鉴权：list readable；mutate/create/delete/move 用 `requireCanMutateTasks`；move 另需任务 accessible。
