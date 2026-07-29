## ADDED Requirements

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

## MODIFIED Requirements

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
