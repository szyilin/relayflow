## Context

接 assigner-web；母 §6.2。实现已在 `TaskAssigneeService` + page scope。

## Goals / Non-Goals

**Goals:** 单测钉死写入规则；contract api ready。

**Non-Goals:** 新端点；新 Flyway；改前端。

## Decisions

1. 无新代码路径时以测试 + 文档收口。
2. ASSIGNED_BY_ME 继续：`assigner_id=me` AND NOT EXISTS me in `task_item_assignee`。
