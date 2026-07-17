# API 契约：workspace-calendar

> **状态**：已冻结（`-api` 已实现）  
> **起草**：`workspace-calendar-v1` / `workspace-calendar-web`  
> **实现**：`workspace-calendar-v1` / `workspace-calendar-api`  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **偏好键**：见下文；持久化走既有 `GET/PUT /app-api/system/user/preference`

## 背景

员工工作台 `/app/calendar`：多日历容器、日程 CRUD、租户内邀约、日/周/月视图。V1.1 增加整日历共享（READ）、RRULE 重复、日/周拖拽改期。

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
| `type` | `PRIMARY` \| `OWNED` \| `SHARED` |
| `ownerUserId` | 所有者 userId（SHARED 时） |
| `permission` | `READ`（仅 type=SHARED） |

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

## REST：日历共享（V1.1）

前缀：`/app-api/calendar/share`

### GET /list

返回当前用户 outgoing + incoming 共享记录。

**Response `data`** 元素：

| 字段 | 说明 |
|------|------|
| `id` | 共享记录 id |
| `calendarId` | 日历 id |
| `calendarName` / `calendarColor` | 日历展示 |
| `granteeUserId` / `granteeNickname` | 被共享者 |
| `ownerUserId` / `ownerNickname` | 所有者 |
| `permission` | `READ`（V1.1 仅只读） |
| `direction` | `OUTGOING` \| `INCOMING` |

### POST /create

```json
{ "calendarId": "9002", "granteeUserId": "2", "permission": "READ" }
```

### DELETE /delete?id=

所有者撤销 outgoing 共享。

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
| `rrule` | iCal RRULE 字符串；无重复时为 null |
| `masterEventId` | 主事件 id（展开实例时） |
| `instanceStart` | 展开实例原始开始时间 |
| `isException` | 是否为例外实例 |
| `recurring` | 是否属于重复系列 |

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
  "attendeeUserIds": ["2"],
  "rrule": "FREQ=WEEKLY;INTERVAL=1;BYDAY=MO;COUNT=10"
}
```

**Response `data`**：新建 event id

### PUT /update

同 create 字段 + `id`；仅组织者。可增删参与人（全量 `attendeeUserIds`，不含组织者自身亦可，服务端保证组织者行存在）。

重复系列编辑可选：

| 字段 | 说明 |
|------|------|
| `editScope` | `THIS` \| `ALL` |
| `instanceStart` | `editScope=THIS` 时必填，目标实例原始开始 |

### DELETE /delete?id=

组织者软删/取消；通知参与人（best-effort）。

Query 可选：`editScope`、`instanceStart`（重复实例删单次时）。

### PUT /reschedule

```json
{
  "id": "1001",
  "startTime": "2026-07-17T19:00:00+08:00",
  "endTime": "2026-07-17T19:30:00+08:00",
  "editScope": "THIS",
  "instanceStart": "2026-07-17T18:30:00+08:00"
}
```

日/周视图拖拽改期/改时长；重复实例默认 `editScope=THIS`。

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
  "dimPastEvents": true,
  "showTaskLayer": true
}
```

前端设置窗分类「日历」编辑上述字段；**不**另开日历页内设置真源。任务图层投影契约见 [`task-calendar-projection/contract.md`](../task-calendar-projection/contract.md)。

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

## 前端实现（V1.1）

Store：`web/src/stores/calendar.ts` → `api/app/calendar.ts`。

- **Share**：侧栏「我管理的」+「共享给我的」；owned 日历「共享」弹层管理 grantee（READ only）
- **RRULE**：编辑器 none/daily/weekly/monthly + count；编辑/删除重复实例时 `editScope` THIS/ALL
- **DnD**：日/周 timed 事件 pointer 拖动 + 底边 resize（组织者、非全天）；15 分钟吸附；乐观 + `PUT /reschedule`；月视图不可拖
