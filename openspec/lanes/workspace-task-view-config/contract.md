# API 契约：workspace-task-view-config

> **状态**：done（integrate：store 走真 API；无 localStorage 真源）  
> **起草**：`workspace-task-view-config-web`  
> **实现**：`workspace-task-view-config-api`  
> **联调**：`workspace-task-view-config-integrate`  
> **母 change**：[`workspace-task-view-model-v1`](../../changes/workspace-task-view-model-v1/proposal.md)  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)

## 背景

每个任务视图上下文一份 **ViewConfig**：`displayMode` / `groupBy` / `sort` / `filters` / `visibleFieldKeys`。列表与看板共用除 `displayMode` 外的配置。

## 鉴权

| 项 | 值 |
|----|-----|
| REST | `Authorization: Bearer <JWT>` |
| 个人 context | 仅本人读写 |
| `LIST` | 成员可读共享默认；`OWNER`/`EDITOR` 可保存；`VIEWER` 不持久化共享配置 |

## contextType

| contextType | contextId | 说明 |
|-------------|-----------|------|
| `MINE` | — | 我负责的 |
| `FOLLOWING` | — | 我关注的 |
| `ALL` | — | 全部任务 |
| `CREATED` | — | 我创建的 |
| `ASSIGNED_BY_ME` | — | 我分配的 |
| `COMPLETED` | — | 已完成 |
| `LIST` | `listId` | 清单 |

## ViewConfig JSON

```json
{
  "displayMode": "LIST",
  "groupBy": null,
  "sort": { "key": "createTime", "order": "DESC" },
  "filters": [],
  "visibleFieldKeys": ["dueTime", "assignee", "status"]
}
```

### groupBy

| 值 | 说明 |
|----|------|
| `null` | 无字段分组 |
| `{ "mode": "FIELD", "fieldKey": "status" }` | 按系统字段；V1 keys：`status` \| `dueTime` \| `assigneeId` |
| `{ "mode": "PERSONAL_CUSTOM" }` | 仅 `MINE`（个人组，后续切片） |
| `{ "mode": "LIST_GROUP" }` | 仅 `LIST`（清单内组，后续切片） |

### sort

| 值 | 说明 |
|----|------|
| `{ "key": "createTime"\|"dueTime"\|"title"\|"status"\|"updateTime", "order": "ASC"\|"DESC" }` | 字段排序 |
| `"MANUAL"` | 拖拽序（看板/列表 rank，后续切片） |

### filters

AND 组合；V1 至少支持：

```json
{ "field": "status", "op": "IN", "values": ["TODO", "IN_PROGRESS"] }
```

叠在 context 默认种子之上。

## REST（草案 · `-api`）

前缀：`/app-api/task/view-config`

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/get` | Query `contextType` + `contextId?` → ViewConfig（无则返回默认） |
| `PUT` | `/save` | Body `{ contextType, contextId?, config }` |

## `-web` 临时行为

~~已删除。~~ Integrate 后 **不得**使用 `USE_LOCAL_VIEW_CONFIG` / localStorage 作为 ViewConfig 真源。

## 错误码

| code | 说明 |
|------|------|
| `TASK_VIEW_CONFIG_FORBIDDEN` | VIEWER 保存 LIST 共享配置 / 非本人改私有 |
| `TASK_LIST_FORBIDDEN` | 非清单成员读 LIST 配置 |
