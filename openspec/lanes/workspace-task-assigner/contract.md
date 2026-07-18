# API 契约：workspace-task-assigner

> **状态**：done（integrate 完成；无本地 Mock；详情读 `assignerId`）  
> **起草**：`workspace-task-assigner-web`  
> **实现**：`workspace-task-assigner-api`  
> **联调**：`workspace-task-assigner-integrate`  
> **母 change**：[`workspace-task-view-model-v1`](../../changes/workspace-task-view-model-v1/proposal.md)  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **相关**：[`workspace-task-quick-views`](../workspace-task-quick-views/contract.md)、[`workspace-task-multi-assignee`](../workspace-task-multi-assignee/contract.md)

## 背景

分配人（`assigner_id`）支撑「我分配的」：`assignerId = 我` **且** 我 **不在** 负责人集合。写入发生在替换负责人集合时（`PUT /assignees` / 兼容 `PUT /assign`），无独立「改分配人」端点。

## 语义（与飞书对齐）

| 操作后集合 | `assignerId` |
|------------|--------------|
| 不含自己、含他人 | 当前操作者 |
| 含自己，或空集合 | `null` |

「我分配的」=`scope=ASSIGNED_BY_ME`：`assigner_id = me` AND NOT EXISTS me in `task_item_assignee`。

## REST

无新增端点。依赖：

| 能力 | 路径 |
|------|------|
| 列表 | `GET /app-api/task/item/page?scope=ASSIGNED_BY_ME` |
| 写集合 | `PUT /app-api/task/item/assignees` |
| 响应字段 | `assignerId`（可 null） |

## 前端

- 详情只读展示分配人（API 字段）
- 左栏「我分配的」；将负责人改为他人且自己不在集合后出现

## 验收

A 将任务负责人设为仅 B → A 在「我分配的」、B 在「我负责的」；详情对双方可见分配人 A。
