## Context

接 mine-groups-web / api；契约已 api ready。

## Goals / Non-Goals

**Goals:** 真 API；删除 Mock；PERSONAL_CUSTOM 激活时拉取 list。

**Non-Goals:** 两用户 E2E 自动化（手动验证即可）。

## Decisions

1. Store 持有 groups + membership；`fetchList` 为唯一加载入口。
2. create/delete/move 乐观更新 + 失败 toast / 回滚。
3. 创建任务后：后端已写默认归属；前端再 `fetchList` 或本地补 membership。
