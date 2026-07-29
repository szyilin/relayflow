# calendar Specification

## Purpose

工作台日历域：日历容器、日程、租户内邀约、整日历共享、重复日程（RRULE）、日/周拖拽改期、Bot 提醒触达，以及 `/app/calendar` 日/周/月视图行为。
## Requirements
### Requirement: Calendar domain module

The system SHALL provide a `calendar` domain with Maven modules `relayflow-module-calendar-api` and `relayflow-module-calendar-biz`, and persist calendar data in tables prefixed with `cal_`.

#### Scenario: Table naming

- **WHEN** a new calendar-domain table is created
- **THEN** its name starts with `cal_`
- **AND** it includes `tenant_id BIGINT NOT NULL` (except platform-global tables if any)

### Requirement: Primary calendar ensure

The system SHALL ensure each active tenant member has exactly one `PRIMARY` calendar in the current tenant (A-class provisioning). Ensure MUST run within the calendar domain (or via calendar domain listener), MUST NOT use a cross-domain god filler that writes `cal_*` from another module's Mapper.

#### Scenario: First access

- **WHEN** a member lists calendars and has no PRIMARY calendar
- **THEN** the system creates a PRIMARY calendar owned by that user (default name such as「我的日历」)
- **AND** subsequent lists return that calendar

#### Scenario: Primary not deletable

- **WHEN** a user attempts to delete their PRIMARY calendar
- **THEN** the system rejects with a business error (e.g. `PRIMARY_CALENDAR_DELETE_FORBIDDEN`)

### Requirement: Owned calendars CRUD

The system SHALL allow a member to create, update (name/color/description), and delete **owned non-primary** calendars in the current tenant.

#### Scenario: Create owned calendar

- **WHEN** a member creates a calendar with a non-empty name and color
- **THEN** the system persists an owned calendar with `owner_user_id` equal to the current user

#### Scenario: Delete non-empty calendar

- **WHEN** a member deletes an owned calendar that still has non-deleted events
- **THEN** the system rejects the delete (V1: refuse rather than cascade)

### Requirement: Personal event CRUD

The system SHALL allow organizers to create, update, and soft-delete events on calendars they own. Events MUST belong to a `calendar_id`, store `start_time`/`end_time` as timestamptz, and support `all_day`. When a recurrence rule is provided, the system SHALL persist it on the master event; events without a rule MUST keep single-instance behavior.

#### Scenario: Create timed event

- **WHEN** a member creates an event on an owned calendar with title and start/end and no RRULE
- **THEN** the event is persisted with `organizer_id` equal to the current user
- **AND** the organizer is recorded as an attendee with role ORGANIZER

#### Scenario: Forbidden edit by non-organizer

- **WHEN** an attendee who is not the organizer attempts to change title or time
- **THEN** the system rejects with `EVENT_FORBIDDEN` (or equivalent)

#### Scenario: Create recurring event

- **WHEN** the organizer creates an event with a supported RRULE
- **THEN** the master event is persisted with the rule
- **AND** list queries in a time window return expanded instances

### Requirement: Event list visibility

For a time range query, the system SHALL return events that are either (1) on calendars owned by the current user, or (2) events where the current user is an attendee (invited), or (3) events on calendars shared to the current user with at least READ permission, within the current tenant. When BUSY_FREE permission is implemented, those shares MUST return only busy blocks without private title or description.

#### Scenario: Invitee sees event without owning calendar

- **WHEN** user B is an attendee of an event on user A's calendar
- **AND** B requests event list covering that time range
- **THEN** the event is included in B's result
- **AND** B is not required to own A's calendar

#### Scenario: Subscriber sees shared calendar events

- **WHEN** user A shares a calendar to user B with READ
- **AND** B requests event list covering events on that calendar
- **THEN** those events are included for B
- **AND** B MUST NOT be allowed to update or delete them unless also organizer

### Requirement: Invite tenant members

The system SHALL allow the organizer to attach zero or more tenant members as attendees when creating or updating an event. Attendees MUST be validated as active members of the current tenant via `system-api` (no direct `sys_*` table access from calendar mappers).

#### Scenario: Invite on create

- **WHEN** the organizer creates an event with attendee user ids
- **THEN** the system persists attendee rows
- **AND** attempts best-effort `ImBotApi.send` with `botCode=calendar-bot` to each invitee (SINGLE)
- **AND** deep link metadata includes `/app/calendar?eventId={id}`

#### Scenario: Attendee respond

- **WHEN** an invited attendee accepts or declines
- **THEN** the system updates that attendee's response status
- **AND** MUST NOT allow changing another user's response

### Requirement: Event reminder via calendar-bot

The calendar module SHALL use `ImBotApi.send` with `botCode=calendar-bot` for start reminders according to the event's remind configuration. Delivery MUST be best-effort and MUST NOT fail event CRUD APIs. The module MUST NOT call removed notify-inbox APIs or write `im_*` tables via calendar mappers.

#### Scenario: Reminder dedupe

- **WHEN** a reminder is sent for an event and user
- **THEN** a stable `dedupeKey` (e.g. `CAL_REMIND:{eventId}:{userId}`) prevents duplicate delivery for the same reminder instance

### Requirement: Workspace calendar page

The `/app/calendar` page SHALL provide day/week/month views, a sidebar with mini-month and owned calendars (checkbox + color), quick-create event UI, and Rail navigation entry. The sidebar MUST include a virtual「我的任务」layer (not a `cal_calendar` row) that can be toggled independently of owned/shared calendars. When that layer is enabled, the grid MUST project the current user's `TODO` tasks that have a `due_time` in the visible range, visually distinguishable from `cal_event` items. Clicking a projected task MUST navigate to `/app/tasks?taskId={id}` (or equivalent deep link). The page MUST NOT persist tasks as `cal_event` rows and MUST NOT open the event editor for projected tasks. The page MUST NOT implement meeting-room resource booking. The page MAY show join-meeting actions for events that carry `meeting_id` (video meetings scheduled via the meeting domain). The sidebar MUST list calendars shared to the user. Day/week views MUST support drag-to-reschedule for organizers (**events only**, not task projections).

#### Scenario: View switch

- **WHEN** a user switches among day, week, and month
- **THEN** the main grid updates without leaving `/app/calendar`

#### Scenario: Quick create

- **WHEN** a user creates an event from the quick-create dialog
- **THEN** the event appears on the grid after save (via store/API)
- **AND** color follows the selected calendar

#### Scenario: Task layer toggle

- **WHEN** a user enables「我的任务」in the calendar sidebar
- **THEN** TODO tasks with `due_time` in the visible range appear on the grid as task projections
- **AND** disabling the layer hides those projections without affecting real calendars

#### Scenario: Click projected task

- **WHEN** a user clicks a projected task on the calendar grid
- **THEN** the client navigates to `/app/tasks?taskId={taskId}`
- **AND** MUST NOT open `CalendarEventEditor` for that item

#### Scenario: Event with linked meeting

- **WHEN** a visible event includes `meeting_id`
- **THEN** the calendar UI MAY expose「加入会议」(or equivalent)
- **AND** MUST NOT treat that as meeting-room inventory booking

### Requirement: Task due projection data source

The calendar workspace UI SHALL load task projections via the task domain app-api (due-range query), not by reading `task_` tables from calendar-biz and not by inventing synthetic `cal_event` ids. Calendar event list APIs MUST NOT be required to embed task rows.

#### Scenario: Parallel fetch when layer on

- **WHEN** the task layer is enabled and the visible range changes
- **THEN** the client requests task due-range for that window (in addition to calendar events)
- **AND** when the layer is disabled, the client MUST NOT require a successful task due-range call to render events

### Requirement: Calendar share and subscribe

The system SHALL allow a calendar owner to grant tenant members access to an entire owned calendar (share). Grantees MUST see the calendar in their calendar list and, for READ permission, see its events in range queries. Share grants MUST be validated via `system-api` membership checks. Owners MUST be able to revoke shares.

#### Scenario: Share create

- **WHEN** the owner shares a calendar to member B with READ
- **THEN** a share grant is persisted
- **AND** B's calendar list includes that calendar as a shared/subscribed entry

#### Scenario: Revoke share

- **WHEN** the owner revokes B's share
- **THEN** B no longer receives that calendar's events via share visibility

### Requirement: Recurring events with exceptions

The system SHALL support recurring events via RRULE (daily/weekly/monthly minimum). Range list MUST expand instances within the requested window. The system MUST support exception instances (cancel or override at least one occurrence) with edit scopes `THIS` and `ALL` (and SHOULD support `THIS_AND_FUTURE`).

#### Scenario: Expand in window

- **WHEN** a master event has a weekly RRULE and the client lists a two-week window
- **THEN** the response includes one instance per matching occurrence in that window

#### Scenario: Cancel single instance

- **WHEN** the organizer deletes or cancels a single occurrence with scope THIS
- **THEN** subsequent lists omit or mark that occurrence without removing the master series

### Requirement: Drag reschedule on calendar grid

The workspace calendar day/week grid SHALL allow organizers to drag an event to change its start time and to resize to change duration, persisting via the calendar event update (or dedicated reschedule) API. Attendees and READ-only subscribers MUST NOT reschedule by drag. For recurring events, drag MUST follow the same edit-scope rules as the editor (default THIS unless the product UI selects otherwise).

#### Scenario: Drag timed event

- **WHEN** an organizer drags a non-recurring timed event to a new start on the week grid
- **THEN** the event's start/end are updated preserving duration (unless resized)
- **AND** a failed save restores the previous position in the UI

### Requirement: Org-level calendar product settings reserved

系统 SHALL 持久化并执行当前租户的日历组织级策略（V1 薄集，见 design：至少 `allowMemberCreateOwnedCalendar`）及全员日历管理能力。管理端产品设置日历入口 MUST 为可配置体验，MUST NOT 再仅展示占位文案。工作台 calendar 相关写路径（如创建 OWNED 日历）MUST 以后端策略为准。个人日历偏好继续由 `user-preference` 的日历段承载；单日历本机显示色由日历域用户偏好表承载，二者均 MUST NOT 被管理端组织设置页直接编辑为个人值。

#### Scenario: Effective policy applies

- **WHEN** 租户已保存日历组织策略（例如禁止成员新建 OWNED 日历）
- **THEN** 相关 calendar App API / 服务端强制执行该策略

#### Scenario: Personal preference unchanged by org page

- **WHEN** 成员在工作台全局设置中编辑日历个人偏好
- **THEN** 行为仍遵循既有 `user-preference` 规格
- **AND** 管理端组织设置页 MUST NOT 作为该个人偏好的写入口

#### Scenario: Admin configures calendar product

- **WHEN** 具备权限的管理员打开 `/admin/products/calendar`
- **THEN** 系统展示可配置的组织策略与/或全员日历管理（非占位）

### Requirement: All-staff calendar type and auto-subscribe

系统 SHALL 支持日历类型 `ALL_STAFF`。全员日历 MUST 仅能由具备日历产品设置写权限的管理员经 Admin API 创建/变更/删除，MUST NOT 由普通成员 App「新建日历」创建。可见范围 MUST 通过 scope 持久化（全员或指定主体），list 时解析；V1 MUST NOT 为范围内每一成员物化 `cal_calendar_share` 行。范围内成员 MUST 自动获得与 READ 共享等价的事件可读可见性。

#### Scenario: Member lists all-staff in scope

- **WHEN** 用户属于某全员日历的可见范围
- **AND** 请求日历 list
- **THEN** 响应包含该日历，来源标识为全员/订阅（非 OWNED）
- **AND** 区间事件查询包含该日历上其有权查看的事件

#### Scenario: Member outside scope

- **WHEN** 用户不在某全员日历可见范围
- **THEN** list 与事件查询 MUST NOT 因该全员日历暴露其内容

#### Scenario: Member cannot create all-staff via app

- **WHEN** 普通成员调用 App 创建日历 API 并试图指定 `ALL_STAFF`
- **THEN** 系统拒绝（忽略类型或返回业务错误）

### Requirement: Per-user display color override

系统 SHALL 允许对用户可见的日历（自有、READ 共享、全员自动订阅）持久化可选的**本机显示色**（每租户每用户每日历至多一行）。有效展示色 MUST 为 `display_color ?? calendar.color`（默认色）。更新本机色 MUST NOT 修改 `cal_calendar.color`。清除本机色（置空）后 MUST 回退到默认色。无日历名称/描述编辑权的用户仍 MUST 被允许设置本机色。

#### Scenario: Default color maps to subscribers

- **WHEN** 所有者或管理员将日历默认色改为 C
- **AND** 订阅者 U 未设置本机色
- **THEN** U 的 list/网格对该日历使用颜色 C

#### Scenario: Personal override is private

- **WHEN** 订阅者 U 将本机色设为 C_u
- **AND** 订阅者 V 未设置本机色
- **THEN** U 看到 C_u，V 仍看到日历默认色
- **AND** `cal_calendar.color` 保持为默认色

#### Scenario: No edit permission still can change display color

- **WHEN** 用户对日历仅有订阅/READ 可见权（不可改名称与描述）
- **AND** 用户提交本机显示色
- **THEN** 系统接受并持久化本机色
- **AND** 若用户试图修改名称或描述，系统拒绝

### Requirement: Workspace calendar row menu and settings panel

工作台 `/app/calendar` 侧栏对「我管理的」与「我订阅的」日历行 SHALL 提供 `···` 菜单，收敛至少：**分享**（仅所有者）、**修改颜色**（本机色快捷）、**日历设置**。MUST 以设置面板（或等价弹层）编辑/查看：名称、描述、颜色、共享人（按角色）。所有者 MUST 能在设置中改默认色与元数据；订阅者 MUST 见只读元数据并可改本机色。侧栏 MUST NOT 再依赖「仅 hover 露出共享+删除两枚裸按钮」作为唯一入口（删除可保留在设置内或菜单，但分享与设置 MUST 经 `···` 可达）。

#### Scenario: Owner opens calendar settings

- **WHEN** 所有者从 `···` 打开日历设置
- **THEN** 可编辑名称、描述、默认颜色，并管理共享人（PRIMARY/OWNED）
- **AND** 保存后 list 反映元数据变更

#### Scenario: Subscriber opens calendar settings

- **WHEN** 订阅者从 `···` 打开日历设置
- **THEN** 名称与描述为只读（或等价不可提交）
- **AND** 颜色控件写入本机色
- **AND** 共享人列表只读（若展示）

#### Scenario: Share via menu

- **WHEN** 所有者选择 `···` → 分享
- **THEN** 可增删 READ 共享（行为与既有 share API 一致）

### Requirement: Sidebar subscribed grouping

工作台日历侧栏 SHALL 区分「我管理的」与「我订阅的」。「我订阅的」MUST 包含经 READ 共享获得的日历与可见范围内的全员日历。客户端 MAY 用类型/来源字段区分展示，MUST NOT 要求用户手动搜索才能看到全员日历。

#### Scenario: All-staff appears under subscribed

- **WHEN** 用户在全员日历可见范围内
- **THEN** 该日历出现在「我订阅的」（或规格等价分组）
- **AND** 不出现在「我管理的」，除非该用户同时为日历所有者/管理员身份且产品将管理入口另列

### Requirement: Cross-domain calendar event API for meeting link

The `calendar-api` module SHALL expose a cross-domain API (e.g. `CalEventApi`) that allows other domains to create or update a calendar event linked to an opaque `meetingId` Long, without those domains writing `cal_*` tables. calendar-biz MUST implement the API by mapping to internal services and MUST NOT depend on `meeting-api` or `meeting-biz`.

#### Scenario: Meeting schedules via API

- **WHEN** meeting-biz calls the calendar cross-domain API to create a linked event
- **THEN** a `cal_event` row is persisted with `meeting_id` set
- **AND** the API returns the new event id to the caller

#### Scenario: No meeting dependency in calendar-biz

- **WHEN** calendar-biz implements the linked-event API
- **THEN** it stores `meeting_id` as an opaque Long
- **AND** does not import meeting domain types or call meeting services

### Requirement: Optional meeting link on calendar events

Calendar events SHALL support an optional `meeting_id`. When present, list/detail responses MUST expose it so the client can offer「加入会议」via meeting app-api. Calendar-biz MUST NOT call LiveKit.

#### Scenario: Event with meeting id

- **WHEN** an event is created or updated with a non-null `meeting_id`
- **THEN** subsequent authorized fetches include that `meeting_id`
- **AND** calendar persistence does not require reading `mtg_*` tables

#### Scenario: Ordinary event unchanged

- **WHEN** a user creates a normal calendar event without scheduling a video meeting
- **THEN** `meeting_id` remains null
- **AND** behavior matches pre-meeting calendar UX

### Requirement: Join meeting entry from calendar

When the workspace calendar UI displays an event that includes `meeting_id`, it MUST offer a control to join or open the associated meeting via meeting routes or APIs. It MUST NOT embed LiveKit credentials in calendar code.

#### Scenario: Click join from event

- **WHEN** a user opens or selects an event that has `meeting_id`
- **THEN** the client can navigate into the meeting join/pre-join flow for that id
- **AND** calendar code does not hold LiveKit API secrets

