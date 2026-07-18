# API 契约：workspace-task-quick-views

> **状态**：draft（`-web` 起草；`-api` 未实现）  
> **起草**：`workspace-task-quick-views-web`  
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

| contextType | 前端 `?view=` | 默认筛选种子 |
|-------------|---------------|--------------|
| `MINE` | （缺省） | 负责人集合包含当前用户；常用再叠未完成（`TODO`/`IN_PROGRESS`） |
| `FOLLOWING` | `following` | 当前用户已关注（既有 `/following/page`） |
| `ALL` | `all` | 可见并集：我负责 ∪ 我创建 ∪ 我关注 ∪ 我为清单成员的任务（**禁止**全租户） |
| `CREATED` | `created` | `creatorId` = 当前用户（既有 `scope=CREATOR`） |
| `ASSIGNED_BY_ME` | `assigned_by_me` | `assignerId` = 当前用户 **且** 当前用户 **不在** 负责人集合 |
| `COMPLETED` | `done` | 完成态 = `DONE`（可见范围同 ALL 或至少 ASSIGNEE∪CREATOR；实现选并集并文档化） |
| `LIST` | `listId=` | 清单成员任务（既有） |

动态 `activity` 仍走活动流 API，不属 page 种子。

## REST 扩展（草案 · `-api`）

前缀：`/app-api/task/item`

### GET /page

在既有 query 上扩展：

| 参数 | 类型 | 说明 |
|------|------|------|
| `scope` | string | 增：`ALL` \| `ASSIGNED_BY_ME`（保留 `ASSIGNEE` \| `CREATOR`） |
| `status` | string | 可选；`COMPLETED` 入口可由前端传 `DONE` 或 scope 暗示 |
| `listId` | string | 有则忽略个人 scope（既有） |

**`ASSIGNED_BY_ME`** 依赖任务 **分配人** 字段（见母 change P4）；未交付前前端用 store 临时数据。

### TaskItem 增量（目标态）

```json
{
  "assignerId": "1"
}
```

可为 `null`（从未指派给他人）。

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

| 入口 | 临时策略（integrate 删除） |
|------|---------------------------|
| `ALL` | Store 合并 ASSIGNEE（无 status）+ CREATOR + FOLLOWING，按 id 去重 |
| `ASSIGNED_BY_ME` | Store 内 mock 列表（含 `assignerId`）；真实 API 前可为空或演示条 |

开关：`USE_LOCAL_QUICK_VIEWS`（store）。

## 错误码

沿用既有 `TASK_*`；无新码本切片。
