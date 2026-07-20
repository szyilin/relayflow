## Context

接 multi-list-web；契约 `openspec/lanes/workspace-task-multi-list/contract.md`。

## Goals / Non-Goals

**Goals:** 表 + 全量替换 API；page/access/union 改成员表；创建写成员；兼容投影 `list_id`=首个。

**Non-Goals:** 前端去 Mock；清单内 group；listMemberships 富对象（可仅 listIds）。

## Decisions

1. 停写业务语义上以 `task_list_item` 为准；`syncProjection` 仍写 `task_item.list_id` 为首个 id（board_rank 索引过渡）。
2. 加入清单：对该 list `requireCanMutateTasks`；移出：操作者须 `requireEditable` 任务，且对移出的 list 若仍是成员则需可 mutate（简化：整次 replace 要求 editable + 每个 **新增** list 可 mutate；移除的 list 不额外要求若已 editable）。
3. 子任务：复制父任务全部 list 成员。
4. Access：任一所属清单的成员可读；任一所属清单 OWNER/EDITOR 可编辑（与 creator/assignee 并列）。
