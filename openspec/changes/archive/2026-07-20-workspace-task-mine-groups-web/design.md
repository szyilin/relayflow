## Context

接 assigner；母 change D5 / §7.1。

## Goals / Non-Goals

**Goals:** 默认组 + 新建/删除；PERSONAL_CUSTOM 分区；拖拽改归属（Mock）。

**Non-Goals:** 真表；他用户可见；清单组 C。

## Decisions

1. Pinia `mineGroups`：groups + taskId→groupId；仅当前浏览器会话（或可 localStorage 可选，本切片内存即可）。
2. 未归属任务分区时落入默认组。
3. 删非默认组：成员改挂默认组。
4. 仅 `navView=mine` 且 `groupBy=PERSONAL_CUSTOM` 启用。

## Risks

Mock 刷新丢失 → `-api`。
