## Context

按 `openspec/lanes/workspace-task-quick-views/contract.md` 实现。V1 仍单 `assignee_id`；多负责人后置。

## Goals / Non-Goals

**Goals:** ALL 可见并集查询；ASSIGNED_BY_ME + assigner 落库与指派写入。

**Non-Goals:** 关 `USE_LOCAL_QUICK_VIEWS`（integrate）；多负责人；ViewConfig。

## Decisions

1. ALL：`assignee ∪ creator ∪ following ∪ list_member(非归档清单)`，根任务 only；可选 status。
2. ASSIGNED_BY_ME：`assigner_id = me AND assignee_id IS DISTINCT FROM me`。
3. 指派给他人 → `assigner_id = 操作者`；指派给自己 → `assigner_id = null`。
4. 自定义 SQL 放 `TaskItemExtMapper`（不改 codegen 业务语义进 Ext）。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| ALL 无索引慢 | 跟既有 tenant+assignee/list 索引；后续可加 |
| 历史无 assigner | 旧数据「我分配的」为空直至新指派 |

## Open Questions

（无）
