## Context

接 multi-assignee；母 change D8 / §6.1。后端 `assigner_id` 与 `scope=ASSIGNED_BY_ME` 已有。

## Goals / Non-Goals

**Goals:** 详情可见分配人；contract 固化语义；「我分配的」空态说明。

**Non-Goals:** 新 REST；改写入规则（已由 `TaskAssigneeService` 实现）。

## Decisions

1. 分配人只读展示（不单独编辑控件；改负责人集合即更新分配人）。
2. contract 引用 quick-views 写入表 + multi-assignee 集合规则。
3. 无本地 Mock 开关（字段已来自 API）。
