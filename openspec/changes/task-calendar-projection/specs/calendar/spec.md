## MODIFIED Requirements

### Requirement: Workspace calendar page

The `/app/calendar` page SHALL provide day/week/month views, a sidebar with mini-month and owned calendars (checkbox + color), quick-create event UI, and Rail navigation entry. The sidebar MUST include a virtual「我的任务」layer (not a `cal_calendar` row) that can be toggled independently of owned/shared calendars. When that layer is enabled, the grid MUST project the current user's `TODO` tasks that have a `due_time` in the visible range, visually distinguishable from `cal_event` items. Clicking a projected task MUST navigate to `/app/tasks?taskId={id}` (or equivalent deep link). The page MUST NOT persist tasks as `cal_event` rows, MUST NOT open the event editor for projected tasks, and MUST NOT implement meeting-room or video-meeting booking UI. The sidebar MUST list calendars shared to the user. Day/week views MUST support drag-to-reschedule for organizers (**events only**, not task projections).

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

## ADDED Requirements

### Requirement: Task due projection data source

The calendar workspace UI SHALL load task projections via the task domain app-api (due-range query), not by reading `task_` tables from calendar-biz and not by inventing synthetic `cal_event` ids. Calendar event list APIs MUST NOT be required to embed task rows.

#### Scenario: Parallel fetch when layer on

- **WHEN** the task layer is enabled and the visible range changes
- **THEN** the client requests task due-range for that window (in addition to calendar events)
- **AND** when the layer is disabled, the client MUST NOT require a successful task due-range call to render events
