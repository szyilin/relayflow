# API 契约：workspace-task-collab

> **状态**：`-web` ui_ready（待 collab-api）  
> **起草**：`workspace-task-core-v1` / `workspace-task-collab-web`  
> **母 change**：[`openspec/changes/workspace-task-core-v1`](../../changes/workspace-task-core-v1/proposal.md)  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **既有**：[`workspace-tasks/contract.md`](../workspace-tasks/contract.md)、[`workspace-task-detail/contract.md`](../workspace-task-detail/contract.md)

## 背景

`/app/tasks` P1：关注人、「我关注的」、评论、任务/个人动态、指派负责人。

## 鉴权

| 项 | 值 |
|----|-----|
| REST | `Authorization: Bearer <JWT>` |
| 产品面 | 有效组织成员；**不用** `sys_permission` |
| 写权限 | 关注/评论：负责人、创建人、已关注人；指派：负责人或创建人；关注人不可改核心字段 |

## 公共类型

### TaskFollower

```json
{
  "userId": "2",
  "nickname": "李四",
  "avatarText": "李",
  "followTime": "2026-07-17T16:00:00+08:00"
}
```

### TaskComment

```json
{
  "id": "9001",
  "taskId": "1001",
  "authorId": "1",
  "authorNickname": "张三",
  "content": "补充附件后再交",
  "createTime": "2026-07-17T16:10:00+08:00"
}
```

### TaskActivity

```json
{
  "id": "8001",
  "taskId": "1001",
  "taskTitle": "整理周报",
  "actorId": "1",
  "actorNickname": "张三",
  "type": "commented",
  "summary": "添加了评论",
  "createTime": "2026-07-17T16:10:00+08:00"
}
```

`type`：`created` | `field_changed` | `subtask_created` | `subtask_done` | `follower_added` | `follower_removed` | `commented` | `assigned`

## REST

前缀：`/app-api/task`

### 关注

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/item/follow` | Body `{ "taskId" }`；幂等 |
| `POST` | `/item/unfollow` | Body `{ "taskId" }` |
| `GET` | `/item/followers?taskId=` | 该任务关注人列表 |
| `GET` | `/item/following/page` | 「我关注的」任务分页（形状同 item page） |

### 指派

| 方法 | 路径 | 说明 |
|------|------|------|
| `PUT` | `/item/assign` | Body `{ "id", "assigneeId" }`；校验同租户有效成员；`task-bot` best-effort |

成功后 `TaskItem.assigneeId` 更新。

### 评论

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/item/comments?taskId=` | 按时间升序 |
| `POST` | `/item/comment/create` | Body `{ "taskId", "content" }`；`data` = comment id |

发评论同时写一条 `commented` activity。

### 动态

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/item/activities?taskId=` | 任务详情内活动流（新→旧或旧→新由实现定，UI 新在上） |
| `GET` | `/activity/feed` | 个人「动态」：本人创建/负责/关注任务的活动，新→旧；Query `limit` 默认 50、最大 100 |

## 错误码（增补）

| code | 说明 |
|------|------|
| `TASK_NOT_FOUND` | 任务不存在 |
| `TASK_FORBIDDEN` | 无权操作 |
| `TASK_ASSIGNEE_NOT_MEMBER` | 指派对象非本租户有效成员 |
| `TASK_COMMENT_EMPTY` | 评论内容为空 |

## 前端行为

| 项 | 约定 |
|----|------|
| 左导航 | 「我负责的」「我关注的」「动态」均可点（非永久占位） |
| 深链 | `?taskId=` 仍打开详情；可选 `?view=following\|activity` |
| 详情 | 负责人可改；关注人 chips + 关注/取消；评论列表+输入；活动流 |
| `-web` 临时 | API 未就绪时 store 用 localStorage 覆盖协作数据；integrate 删除 |

## curl 示例

```bash
curl -sS -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"taskId":"1001"}' "$BASE/app-api/task/item/follow"

curl -sS -H "Authorization: Bearer $TOKEN" \
  "$BASE/app-api/task/activity/feed?limit=50"
```
