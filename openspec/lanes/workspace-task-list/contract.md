# API 契约：workspace-task-list

> **状态**：api ready（`-web` UI + contract；`-api` 已实现；待 integrate 去本地临时数据）  
> **起草**：`workspace-task-list-board-v1` / `workspace-task-list-web`  
> **母 change**：[`openspec/changes/workspace-task-list-board-v1`](../../changes/workspace-task-list-board-v1/proposal.md)  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **既有**：[`workspace-tasks`](../workspace-tasks/contract.md)、[`workspace-task-detail`](../workspace-task-detail/contract.md)、[`workspace-task-collab`](../workspace-task-collab/contract.md)

## 背景

`/app/tasks` P0：任务清单容器、成员、任务可选 `listId` 归属；左栏「清单」导航与深链 `?listId=`。看板三列见后续 `workspace-task-board`。

## 鉴权

| 项 | 值 |
|----|-----|
| REST | `Authorization: Bearer <JWT>` |
| 产品面 | 有效组织成员；**不用** `sys_permission` |
| 清单读 | 当前用户为该清单 `task_list_member` |
| 清单写（改名/归档/邀成员） | 仅 `OWNER` |
| 清单内任务写 | `OWNER` / `EDITOR`，或任务 assignee/creator（并集） |
| VIEWER | 可读 + 可评论（对齐 follower）；不可改清单/拖状态/改核心字段 |

## 公共类型

### TaskList

```json
{
  "id": "5001",
  "name": "产品发布",
  "description": "Q3 发布相关",
  "ownerId": "1",
  "archived": false,
  "myRole": "OWNER",
  "createTime": "2026-07-17T20:00:00+08:00"
}
```

`myRole`：`OWNER` | `EDITOR` | `VIEWER`

### TaskListMember

```json
{
  "userId": "2",
  "nickname": "李四",
  "avatarText": "李",
  "role": "EDITOR",
  "joinTime": "2026-07-17T20:05:00+08:00"
}
```

### TaskItem（增量字段）

在既有 TaskItem 上增加：

```json
{
  "listId": "5001"
}
```

`listId` 可为 `null`（个人池）。

## REST

前缀：`/app-api/task`

### 清单

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/list/mine` | 我参与的未归档清单（含 `myRole`） |
| `POST` | `/list/create` | Body `{ "name", "description?" }`；同事务写 OWNER；`data` = list id |
| `PUT` | `/list/update` | Body `{ "id", "name?", "description?" }`；仅 OWNER |
| `POST` | `/list/archive` | Body `{ "id" }`；仅 OWNER；不删任务 |
| `GET` | `/list/get` | Query `id`；非成员 → `TASK_LIST_FORBIDDEN` 或 `TASK_LIST_NOT_FOUND`（contract：**FORBIDDEN**） |

### 成员

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/list/member/list` | Query `listId` |
| `POST` | `/list/member/invite` | Body `{ "listId", "userId", "role" }`；`role`=`EDITOR`\|`VIEWER`；仅 OWNER；校验租户 ACTIVE 成员 |
| `PUT` | `/list/member/update-role` | Body `{ "listId", "userId", "role" }`；仅 OWNER；不可降级唯一 OWNER |
| `POST` | `/list/member/remove` | Body `{ "listId", "userId" }`；仅 OWNER；不可移除唯一 OWNER |

重复邀请同一用户：**幂等**（已存在则更新 role 或 no-op；实现选幂等更新 role）。

### 任务归属（扩展既有 item API）

| 方法 | 路径 | 变更 |
|------|------|------|
| `GET` | `/item/page` | Query 增 `listId?`：有则返回该清单根任务（成员可见范围）；无则保持「我负责的」等既有 scope |
| `POST` | `/item/create` | Body 增 `listId?`；有权限时写入；子任务创建时继承父 `listId` |
| `GET` | `/item/get` | 响应含 `listId`；清单任务须为成员才可读 |

## 错误码

| code | 说明 |
|------|------|
| `TASK_LIST_NOT_FOUND` | 清单不存在 |
| `TASK_LIST_FORBIDDEN` | 非成员或角色不足 |
| `TASK_LIST_NAME_EMPTY` | 名称为空 |
| `TASK_LIST_MEMBER_NOT_TENANT` | 邀请对象非本租户有效成员 |
| `TASK_LIST_OWNER_REQUIRED` | 操作需要保留至少一名 OWNER |
| `TASK_FORBIDDEN` | 任务写权限不足（沿用） |

## 前端行为

| 项 | 约定 |
|----|------|
| 左栏 | 个人入口下方「清单」分组：我的清单 + 新建 |
| 深链 | `?listId=` 进入清单上下文；可与 `?taskId=` 同用 |
| 切回个人 | 清除 `listId` query；恢复 `view` 语义 |
| 清单中栏 | 根任务列表；新建默认带当前 `listId`；成员管理（OWNER 可邀） |
| 看板 Tab | 本切片可保留占位文案；完整看板见 board 切片 |
| `-web` 临时 | store 内本地清单/成员/清单任务可撑 UI；**integrate 删除** |
| 真源（integrate 后） | 仅走 API；无 localStorage 覆盖 |

## curl 示例

```bash
# 我的清单
curl -s -H "Authorization: Bearer $TOKEN" \
  "$BASE/app-api/task/list/mine"

# 创建清单
curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"产品发布"}' \
  "$BASE/app-api/task/list/create"

# 清单内任务
curl -s -H "Authorization: Bearer $TOKEN" \
  "$BASE/app-api/task/item/page?listId=5001&pageNo=1&pageSize=20"

# 邀请成员
curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"listId":"5001","userId":"2","role":"EDITOR"}' \
  "$BASE/app-api/task/list/member/invite"
```

## 浏览器验证（`-web`）

1. `/app/login` → `/app/tasks`
2. 左栏「清单」可见种子清单；点选进入中栏列表
3. 新建清单 → 左栏出现；中栏可新建任务（进当前清单）
4. OWNER 打开成员 → 邀请通讯录成员（本地临时）
5. `?listId=` 深链进入对应清单；个人入口行为不变；详情 slideover 仍可打开（清单临时任务用本地详情）
