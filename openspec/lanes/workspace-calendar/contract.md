# API 契约：workspace-calendar

> **状态**：已冻结（`-api` 已实现）  
> **起草**：`workspace-calendar-v1` / `workspace-calendar-web`  
> **实现**：`workspace-calendar-v1` / `workspace-calendar-api`  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **偏好键**：见下文；持久化走既有 `GET/PUT /app-api/system/user/preference`

## 背景

员工工作台 `/app/calendar`：多日历容器、日程 CRUD、租户内邀约、日/周/月视图。V1 **无**整日历共享、RRULE、会议室、任务虚拟图层。

## 鉴权

| 项 | 值 |
|----|-----|
| REST | `Authorization: Bearer <JWT>` |
| 产品面 | 有效组织成员；**不用** `sys_permission` |

## 时间约定

- 存库 / API：`TIMESTAMPTZ` → JSON ISO-8601（含偏移，如 `2026-07-17T10:00:00+08:00`）
- `allDay=true`：`startTime`/`endTime` 仍为 timestamptz；语义为本地日历日 `[startDate, endDate)`（end 为结束日次日 00:00 本地，或与产品约定 end 为结束日 23:59:59 — **实现采用：全天事件 end 为结束日次日 00:00 本地的瞬时**）
- 区间 list：`from` 含、`to` 不含（半开区间），返回与 `[from,to)` 有交集的事件

## REST：日历

前缀：`/app-api/calendar/calendar`

### GET /list

首次调用若无 PRIMARY，服务端 ensure 后返回。

**Response `data`**：

```json
[
  {
    "id": "9001",
    "name": "我的日历",
    "color": "#3B82F6",
    "description": null,
    "type": "PRIMARY"
  }
]
```

| 字段 | 说明 |
|------|------|
| `type` | `PRIMARY` \| `OWNED` |

### POST /create

```json
{ "name": "项目排期", "color": "#EC4899", "description": "" }
```

**Response `data`**：新建日历 id（string/number）

### PUT /update

```json
{ "id": "9002", "name": "项目排期", "color": "#EC4899", "description": "备注" }
```

### DELETE /delete?id=

PRIMARY 拒绝；OWNED 且仍有未删事件拒绝。

## REST：日程

前缀：`/app-api/calendar/event`

### GET /list

**Query**：

| 参数 | 必填 | 说明 |
|------|------|------|
| `from` | 是 | ISO 区间起点 |
| `to` | 是 | ISO 区间终点（不含） |
| `calendarIds` | 否 | 逗号分隔；仅过滤**自有日历**上的事件；邀请事件仍返回 |

**Response `data`**：

```json
[
  {
    "id": "1001",
    "calendarId": "9001",
    "calendarColor": "#3B82F6",
    "calendarName": "我的日历",
    "title": "周会",
    "description": "",
    "startTime": "2026-07-17T18:30:00+08:00",
    "endTime": "2026-07-17T19:00:00+08:00",
    "allDay": false,
    "organizerId": "1",
    "remindBeforeMinutes": 5,
    "allDayRemindTime": null,
    "status": "CONFIRMED",
    "viewerRole": "ORGANIZER",
    "attendees": [
      { "userId": "1", "role": "ORGANIZER", "response": "ACCEPTED", "nickname": "我" },
      { "userId": "2", "role": "ATTENDEE", "response": "NEEDS_ACTION", "nickname": "同事" }
    ],
    "invitedOnly": false
  }
]
```

| 字段 | 说明 |
|------|------|
| `viewerRole` | 当前用户相对该事件：`ORGANIZER` \| `ATTENDEE` |
| `invitedOnly` | true 表示当前用户不拥有该 `calendarId`（受邀可见） |

### GET /get?id=

单条详情（字段同 list 元素）。

### POST /create

```json
{
  "calendarId": "9001",
  "title": "周会",
  "description": "",
  "startTime": "2026-07-17T18:30:00+08:00",
  "endTime": "2026-07-17T19:00:00+08:00",
  "allDay": false,
  "remindBeforeMinutes": 5,
  "allDayRemindTime": null,
  "attendeeUserIds": ["2"]
}
```

**Response `data`**：新建 event id

### PUT /update

同 create 字段 + `id`；仅组织者。可增删参与人（全量 `attendeeUserIds`，不含组织者自身亦可，服务端保证组织者行存在）。

### DELETE /delete?id=

组织者软删/取消；通知参与人（best-effort）。

### PUT /respond

```json
{ "id": "1001", "response": "ACCEPTED" }
```

`response`：`ACCEPTED` \| `DECLINED`；仅 ATTENDEE。

## 用户偏好（日历段）

既有：`GET/PUT /app-api/system/user/preference`

`settings.calendar` 默认：

```json
{
  "weekStartsOn": 0,
  "defaultEventDurationMinutes": 30,
  "defaultRemindBeforeMinutes": 5,
  "allDayRemindTime": "08:00",
  "dimPastEvents": true
}
```

前端设置窗分类「日历」编辑上述字段；**不**另开日历页内设置真源。

## 错误码（calendar 域）

| code | 说明 |
|------|------|
| `1_005_001_001` | 日历不存在 |
| `1_005_001_002` | 无权操作该日历 |
| `1_005_001_003` | 主日历不可删除 |
| `1_005_001_004` | 日历下仍有日程 |
| `1_005_002_001` | 日程不存在 |
| `1_005_002_002` | 无权操作该日程 |
| `1_005_002_003` | 日程时间无效 |
| `1_005_003_001` | 参与人无效 |

## curl 示例

```bash
curl -sS -H "Authorization: Bearer $TOKEN" \
  "$BASE/app-api/calendar/calendar/list"

curl -sS -H "Authorization: Bearer $TOKEN" \
  "$BASE/app-api/calendar/event/list?from=2026-07-12T00:00:00%2B08:00&to=2026-07-19T00:00:00%2B08:00"
```

## 前端临时实现（-web）

- Store：`web/src/stores/calendar.ts`（内存临时数据；integrate 删除）
- API：`web/src/api/app/calendar.ts`
- 页面：`/app/calendar`；深链 `?eventId=` / `?date=`
