# API 契约：workspace-tasks

> **状态**：已对接（integrate）  
> **起草**：`workspace-tasks-v1` change  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)

## 背景

员工工作台 `/app/tasks`：V1 仅 **我负责的** 任务 CRUD（assignee = 当前用户）。

## 鉴权

| 项 | 值 |
|----|-----|
| REST | `Authorization: Bearer <JWT>` |
| 产品面 | 有效组织成员；**不用** `sys_permission` |

## REST 端点

前缀：`/app-api/task/item`

### GET /page

**Query**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `pageNo` | number | 否 | 默认 1 |
| `pageSize` | number | 否 | 默认 20 |
| `status` | string | 否 | `TODO` \| `DONE` |

**Response `data`**：

```json
{
  "list": [
    {
      "id": 1001,
      "title": "整理周报",
      "status": "TODO",
      "dueTime": "2026-07-15T18:00:00+08:00",
      "createTime": "2026-07-12T10:00:00+08:00"
    }
  ],
  "total": 1
}
```

### POST /create

**Body**：

```json
{ "title": "整理周报", "dueTime": "2026-07-15T18:00:00+08:00" }
```

**Response `data`**：新建任务 id（number）

### PUT /update

**Body**：

```json
{ "id": 1001, "title": "整理周报（改）", "dueTime": null }
```

### PUT /toggle-done

**Body**：

```json
{ "id": 1001, "done": true }
```

### DELETE /delete

**Query**：`id`

## 错误码

| code | 说明 |
|------|------|
| `TASK_NOT_FOUND` | 任务不存在 |
| `TASK_FORBIDDEN` | 非本人任务 |

## 浏览器验证（-web Mock / integrate 真实 API）

1. `/app/login` → `/app/tasks`
2. 点击「新建」→ 填写标题 → 列表出现
3. 勾选完成 → 样式变灰/删除线
