# API 契约：workspace-task-quick-views

> **状态**：done（integrate：store 无本地临时；`ALL` / `ASSIGNED_BY_ME` 走真 API）  
> **起草**：`workspace-task-quick-views-web`  
> **实现**：`workspace-task-quick-views-api`  
> **联调**：`workspace-task-quick-views-integrate`  
> **母 change**：[`workspace-task-view-model-v1`](../../changes/workspace-task-view-model-v1/proposal.md)  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **既有**：[`workspace-tasks`](../workspace-tasks/contract.md)、[`workspace-task-collab`](../workspace-task-collab/contract.md)

## 背景

`/app/tasks` 左栏 **快速访问** 与个人入口是**预设查询上下文**，不是清单。本契约描述 `contextType`、默认筛选种子，以及 `GET /item/page` 扩展（`-api` 实现）。

## 鉴权

| 项 | 值 |
|----|-----|
| REST | `Authorization: Bearer <JWT>` |
| 产品面 | 有效组织成员；**不用** `sys_permission` |

## 上下文与默认种子

快捷入口 = 导航上下文 + **写入 ViewConfig.filters 的默认可编辑种子**（筛选面板可见）。无保存配置时 `GET view-config` 返回种子；用户改 filters 后按保存值查询，进入口不覆盖。

| contextType | 前端 `?view=` | 默认 filters 种子（V1） | 列表底数据（-web） |
|-------------|---------------|-------------------------|-------------------|
| `MINE` | （缺省） | `assigneeId CONTAINS __ME__` + `status IN [TODO,IN_PROGRESS]` | `scope=ALL`，客户端按 filters 收窄 |
| `FOLLOWING` | `following` | `[]`（关注关系由 `/following/page`） | following page |
| `ALL` | `all` | `[]`；可见并集（**禁止**全租户） | `scope=ALL` |
| `CREATED` | `created` | `creatorId EQ __ME__` | `scope=ALL` + filters |
| `ASSIGNED_BY_ME` | `assigned_by_me` | `assignerId EQ __ME__` + `assigneeId NOT_CONTAINS __ME__` | `scope=ALL` + filters |
| `COMPLETED` | `done` | `status IN [DONE]` | `scope=ALL` + filters |
| `LIST` | `listId=` | `[]` | 清单 page |

动态 `activity` 仍走活动流 API，不属 page 种子。

> 历史：种子曾仅靠 `scope=CREATOR` / `status=DONE` 等隐式表达；现改为显式 filters。服务端按 clause 过滤另开 `-api`。

## REST 扩展（草案 · `-api`）

前缀：`/app-api/task/item`

### GET /page

在既有 query 上扩展：

| 参数 | 类型 | 说明 |
|------|------|------|
| `scope` | string | 增：`ALL` \| `ASSIGNED_BY_ME`（保留 `ASSIGNEE` \| `CREATOR`） |
| `status` | string | 可选；`COMPLETED` 入口可由前端传 `DONE` 或 scope 暗示 |
| `listId` | string | 有则忽略个人 scope（既有） |

**`ASSIGNED_BY_ME`** 依赖 `task_item.assigner_id`（本 api 切片已落库；指派给他人时写入操作者，指派给自己时清空）。

### TaskItem 增量

```json
{
  "assignerId": "1"
}
```

可为 `null`（从未指派给他人，或已收回给自己）。

### 指派写入规则

| 操作 | `assignee_id` | `assigner_id` |
|------|---------------|---------------|
| A 指派给 B（A≠B） | B | A |
| A 指派给自己 | A | `null` |

## 前端深链

| 路径 | 说明 |
|------|------|
| `/app/tasks` | 我负责的 |
| `/app/tasks?view=all` | 全部任务 |
| `/app/tasks?view=created` | 我创建的 |
| `/app/tasks?view=assigned_by_me` | 我分配的 |
| `/app/tasks?view=done` | 已完成 |
| `/app/tasks?view=following` | 我关注的 |
| `/app/tasks?listId={id}` | 清单（优先于 view） |

## curl 示例（`-api` 就绪后）

```bash
# 全部任务（可见并集）
curl -s -H "Authorization: Bearer $TOKEN" \
  "$BASE/app-api/task/item/page?pageNo=1&pageSize=20&scope=ALL"

# 我分配的
curl -s -H "Authorization: Bearer $TOKEN" \
  "$BASE/app-api/task/item/page?pageNo=1&pageSize=20&scope=ASSIGNED_BY_ME"
```

## `-web` 临时行为

~~已删除。~~ Integrate 后 store **不得**保留 `USE_LOCAL_QUICK_VIEWS` / 客户端合并 Mock。

| 入口 | 真 API |
|------|--------|
| `ALL` | `scope=ALL` |
| `ASSIGNED_BY_ME` | `scope=ASSIGNED_BY_ME` |
| `COMPLETED`（已完成） | `scope=ALL&status=DONE` |

## 错误码

沿用既有 `TASK_*`；无新码本切片。
