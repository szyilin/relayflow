# API 契约：workspace-task-board

> **状态**：ui_ready（`-web` 草稿；board-api 未实现时前端可本地暂存拖拽）  
> **起草**：`workspace-task-list-board-v1` / `workspace-task-board-web`  
> **母 change**：[`openspec/changes/workspace-task-list-board-v1`](../../changes/workspace-task-list-board-v1/proposal.md)  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **前置**：[`workspace-task-list`](../workspace-task-list/contract.md)

## 背景

清单上下文「列表 | 看板」：三列 = `TODO` / `IN_PROGRESS` / `DONE`；拖拽改状态与列内序（`boardRank`）。个人入口不提供完整看板。

## 鉴权

| 项 | 值 |
|----|-----|
| REST | `Authorization: Bearer <JWT>` |
| 看板读 | 清单成员 |
| 拖拽 / 改状态 | `OWNER` / `EDITOR`（与清单任务写一致）；`VIEWER` 拒绝 |

## 状态三态

| status | 看板列 |
|--------|--------|
| `TODO` | 待办 |
| `IN_PROGRESS` | 进行中 |
| `DONE` | 已完成 |

- 勾选完成 / `toggle-done` done=true → `DONE`
- 取消完成 → `TODO`（不恢复 `IN_PROGRESS`）
- due-range / due Bot：`IN_PROGRESS` 视为未完成（与 `TODO` 同等）

## TaskItem 增量

```json
{
  "status": "IN_PROGRESS",
  "boardRank": 1000
}
```

`boardRank`：同清单同列内排序（升序）；可空，空则按 `createTime`/`id`。

## REST

前缀：`/app-api/task/item`

### PUT /board-move

拖拽落库（跨列或列内重排）。

**Body**：

```json
{
  "id": "1001",
  "status": "IN_PROGRESS",
  "boardRank": 2000
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| `id` | 是 | 根任务 id |
| `status` | 是 | `TODO` \| `IN_PROGRESS` \| `DONE` |
| `boardRank` | 否 | 目标列内序；缺省由服务端插到列尾 |

**Response `data`**：`true`

**错误**：`TASK_FORBIDDEN` / `TASK_LIST_FORBIDDEN` / `TASK_NOT_FOUND`；子任务不可 board-move（仅根任务）。

### GET /page（看板加载）

清单看板建议 `listId` + `pageSize=100`（或后续专用 board 端点）；本切片沿用 page。

### PUT /toggle-done（不变语义）

done=true → `DONE`；done=false → `TODO`。

## 前端行为

| 项 | 约定 |
|----|------|
| 看板 Tab | **仅清单上下文**显示；个人「我负责的」等不显示看板 Tab |
| 列 | 待办 / 进行中 / 已完成 |
| 拖拽 | HTML5 DnD：跨列改 status；列内改 `boardRank` |
| 点卡片 | 打开既有详情 slideover |
| `-web` 临时 | API 未就绪时 store 内本地应用拖拽结果（`USE_LOCAL_BOARD_MOVE`）；**integrate 删除** |
| VIEWER | 可看不可拖 |

## curl 示例

```bash
curl -s -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"id":"1001","status":"IN_PROGRESS","boardRank":1000}' \
  "$BASE/app-api/task/item/board-move"
```

## 浏览器验证（`-web`）

1. `/app/tasks?listId=…` → 切「看板」见三列  
2. 拖卡片到「进行中」→ 列变化（本地暂存或 API）  
3. 点卡片开详情；个人入口无看板 Tab  
4. `pnpm build` + `pnpm typecheck`
