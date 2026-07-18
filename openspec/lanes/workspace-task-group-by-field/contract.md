# API 契约：workspace-task-group-by-field

> **状态**：api ready（`-api` 已实现；`-integrate` 待接前端）  
> **起草**：`workspace-task-group-by-field-web`  
> **实现**：`workspace-task-group-by-field-api`  
> **母 change**：[`workspace-task-view-model-v1`](../../changes/workspace-task-view-model-v1/proposal.md)  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **相关**：[`workspace-task-view-config`](../workspace-task-view-config/contract.md)、[`workspace-task-board`](../workspace-task-board/contract.md)

## 背景

ViewConfig.`groupBy.mode=FIELD` 驱动列表分区与看板列。空值 →「无分组」。跨桶拖拽写回对应系统字段。分桶展示仍由客户端对 page 结果分区（本切片不提供服务端分桶查询端点）。

## 鉴权

与任务写权限一致（`requireEditable`：assignee/creator/list EDITOR+|OWNER）。VIEWER 不可拖。仅根任务可 group-move。

## 分桶规则（V1 系统字段）

| fieldKey | 桶 key | 标签 |
|----------|--------|------|
| `status` | `TODO` / `IN_PROGRESS` / `DONE` | 未开始 / 进行中 / 已完成 |
| `dueTime` | `YYYY-MM-DD` 或 `__empty__` | 日期或「无分组」 |
| `assigneeId` | userId 或 `__empty__` | 用户 id（后续可昵称）或「无分组」 |

稳定空桶 key：`__empty__`，展示名「无分组」。API 亦接受 `value: null` 表示空桶。

## REST

前缀：`/app-api/task/item`

| 方法 | 路径 | 说明 |
|------|------|------|
| `PUT` | `/group-move` | Body `{ id, fieldKey, value, beforeId? }` |
| `PUT` | `/board-move` | **过渡兼容**：仅 status + boardRank；新客户端优先 `group-move` |

### PUT /group-move

```json
{
  "id": "1001",
  "fieldKey": "status",
  "value": "IN_PROGRESS",
  "beforeId": null
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| `id` | 是 | 根任务 id |
| `fieldKey` | 是 | `status` \| `dueTime` \| `assigneeId` |
| `value` | 条件 | status 必填合法态；dueTime=`YYYY-MM-DD`；assigneeId=用户 id 字符串；`null`/`__empty__` 清空 due/assignee |
| `beforeId` | 否 | 仅 status：插到该卡之前；缺省列尾 |

**Response `data`**：`true`

**行为摘录**：
- `status`：写 status + boardRank；不可清空
- `dueTime`：写截止日（保留原时分或默认 12:00）；清空 → null
- `assigneeId`：须本租户有效成员；清空同时清 `assignerId`；指派他人写 `assignerId=当前用户`

## 错误码

| code | 说明 |
|------|------|
| `TASK_FORBIDDEN` | 无权 / 子任务 |
| `TASK_NOT_FOUND` | 任务不存在 |
| `TASK_GROUP_MOVE_INVALID` | 字段/值非法（如清空 status、非法日期） |
| `TASK_ASSIGNEE_NOT_MEMBER` | 负责人非有效成员 |

## curl

```bash
curl -s -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"id":"1001","fieldKey":"status","value":"IN_PROGRESS"}' \
  "$BASE/app-api/task/item/group-move"
```

## `-web` 临时行为（integrate 删除）

`USE_LOCAL_GROUP_MOVE`：仅改 Pinia 内存；integrate 后改走本 API。
