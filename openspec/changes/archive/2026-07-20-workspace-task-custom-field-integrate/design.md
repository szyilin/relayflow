## Context

接 custom-field-api；镜像 list-groups-integrate。

## Goals / Non-Goals

**Goals:** 去 Mock；CRUD/取值/拖拽走 API；build+typecheck。

**Non-Goals:** 新字段类型；改后端合同（除非 blocker）。

## Decisions

1. Store `fetchList` 拉 fields+values；乐观更新失败回滚/重拉。
2. `USE_LOCAL_CUSTOM_FIELD` 删除。
3. group-move 自定义分支走 `groupMoveTask({ listId, fieldKey, value })`。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 刷新后种子字段消失 | 预期：无服务端种子；用户自建 |
