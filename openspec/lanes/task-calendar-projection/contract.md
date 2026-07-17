# API 契约：task-calendar-projection

> **状态**：已冻结（`-api` / integrate 完成）  
> **起草**：`task-calendar-projection` / `-web`  
> **实现**：`task-calendar-projection`（母 change §2–§3）  
> **母 change**：[`openspec/changes/task-calendar-projection`](../../changes/task-calendar-projection/proposal.md)  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)

## 背景

日历 `/app/calendar` 侧栏「我的任务」**虚拟图层**：将当前用户带 `due_time` 的 `TODO` 投影到日/周/月网格。  
**分域**：不写 `cal_event`；前端并行拉任务 due-range + 既有 event list。

## 鉴权

| 项 | 值 |
|----|-----|
| REST | `Authorization: Bearer <JWT>` |
| 产品面 | 有效组织成员；**不用** `sys_permission` |

## 时间约定

- `from` / `to`：ISO-8601 含偏移；半开区间 **`[from, to)`**
- `due_time` 落在该区间内则返回（与日历 list 区间约定一致）

## REST

前缀：`/app-api/task/item`（扩展既有 workspace-tasks）

### GET /due-range

**Query**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `from` | string | 是 | 窗口起点（含） |
| `to` | string | 是 | 窗口终点（不含） |
| `limit` | number | 否 | 默认 200，最大 200 |

**语义**：

- `assignee_id` = 当前用户
- `tenant_id` = JWT tenant
- `status` = `TODO`
- `due_time` ∈ `[from, to)`
- 无 `due_time`、已 `DONE`、他人任务：**不返回**

**Response `data`**：

```json
[
  {
    "id": "1001",
    "title": "整理周报",
    "status": "TODO",
    "dueTime": "2026-07-17T18:00:00+08:00"
  }
]
```

| 字段 | 说明 |
|------|------|
| `id` | 任务 id（string/number 均可；前端规范化为 string） |
| `title` | 标题 |
| `status` | 恒为 `TODO`（本接口） |
| `dueTime` | ISO-8601；必有 |

超出 `limit` 时截断返回，仍 `code=0`（可不返回 truncated 标记；前端月视图自行折叠展示）。

## 用户偏好

既有：`GET/PUT /app-api/system/user/preference`

`settings.calendar` 增补：

| 键 | 类型 | 默认 | 说明 |
|----|------|------|------|
| `showTaskLayer` | boolean | `true` | 进入 `/app/calendar` 时是否默认勾选「我的任务」图层 |

侧栏勾选仅影响**当前会话**；设置窗开关写回 preference 作为下次进入默认值。

## 前端行为（合同）

| 项 | 约定 |
|----|------|
| 图层开启 | 并行请求 due-range（与 event list）；关闭则不依赖 due-range 成功 |
| 点击投影 | `router.push({ path: '/app/tasks', query: { taskId } })` |
| 禁止 | 打开 `CalendarEventEditor`；拖任务改期；写入 `cal_event` |
| 视觉 | 与日程可区分（固定任务色 + 勾选语义） |

## curl 示例

```bash
curl -sS -H "Authorization: Bearer $TOKEN" \
  "$BASE/app-api/task/item/due-range?from=2026-07-12T00:00:00%2B08:00&to=2026-07-19T00:00:00%2B08:00"
```

## `-web` 临时策略

（已移除）Store 仅调用本契约 `GET /due-range`，无 page 过滤回退。
