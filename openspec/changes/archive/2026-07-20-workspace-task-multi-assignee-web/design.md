## Context

接 group-by-field；母 change D2/D9。

## Goals / Non-Goals

**Goals:** 详情多选负责人；空集合允许；MINE 语义文档化；本地 Mock 写集合。

**Non-Goals:** `task_item_assignee` 表；改 page/Bot/日历；指派 Bot。

## Decisions

1. `assigneeIds: string[]` 真源（前端）；`assigneeId` = 首个元素（兼容分组/旧展示）。
2. `USE_LOCAL_MULTI_ASSIGNEE`：`setAssignees` 只改 Pinia；刷新丢失。
3. 指派弹层改为多选勾选 + 保存；可含自己；可清空。
4. 「我负责的」列表：本地若当前用户不在 `assigneeIds` 则从 mine 列表移除（演示移出效果）。

## Risks

Mock 与后端单列不一致 → `-api`/`-integrate` 解决。
