## Context

接 multi-list；母 change D7 / §9.1。

## Goals / Non-Goals

**Goals:** 每清单默认组；LIST_GROUP（及清单内无 FIELD 时）分区；建删组、拖拽 Mock。

**Non-Goals:** 真表；详情改 C 组（可后置）；字段分组并存验收（integrate）。

## Decisions

1. Pinia `listGroups`：按 `listId` 隔离 groups + task→groupId。
2. `isListGroupActive` = 清单上下文且 `groupBy` 非 FIELD（null 或 LIST_GROUP）。
3. 删非默认组 → 成员回默认组。
4. 镜像 mine-groups UI 条。
