## Context

前端优先切片；母 change 见 `workspace-task-view-model-v1`。既有 `mine` / `created` / `done` / `following` / `activity` 与 `getTaskPage(scope)`。

## Goals / Non-Goals

**Goals:** 左栏信息架构对齐飞书；各快捷入口可点开并展示列表；contract 写清 contextType 与默认种子。

**Non-Goals:** 真分配人落库、ViewConfig 持久化、字段分组看板、多清单；不改后端。

## Decisions

1. Query：`?view=all|assigned_by_me|created|done|following|activity`；默认无 view = mine；`listId` 优先于 view。
2. `ALL` / `ASSIGNED_BY_ME`：`USE_LOCAL_QUICK_VIEWS` 临时；ALL 可合并现有 API 结果近似，ASSIGNED_BY_ME 用本地 mock（无 assigner 字段前）。
3. 清单区块保持现状；清单文件夹后置。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| ALL 本地合并 ≠ 最终可见范围 | contract 写死目标并集；UI 标注临时 |
| ASSIGNED_BY_ME mock 无真实数据 | integrate/assigner 切片替换 |

## Open Questions

- 「全部任务」默认是否叠「未完成」：本切片 UI 默认展示未完成+进行中优先，工具栏筛选后置。
