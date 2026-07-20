## Context

接 list-groups-web / api；契约已 api ready。D7：FIELD 优先于 LIST_GROUP。

## Goals / Non-Goals

**Goals:** 真 API；删除 Mock；清单上下文且 list-group 激活时按 listId 拉取。

**Non-Goals:** 多用户并发 E2E 自动化（手动验证即可）。

## Decisions

1. Store 按 listId 缓存 groups + membership；`fetchList(listId)` 为加载入口。
2. create/delete/move 乐观更新 + 失败 toast / 回滚。
3. 创建任务后：后端已写默认 group_id；前端 `ensureTaskInDefault` + `fetchList`。
4. 切换清单或激活 LIST_GROUP / null groupBy 时重新拉取。
