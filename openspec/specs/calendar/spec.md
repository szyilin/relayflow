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

The `/app/calendar` page SHALL provide day/week/month views, a sidebar with mini-month and owned calendars (checkbox + color), quick-create event UI, and Rail navigation entry. The page MUST NOT show a「我的任务」virtual calendar layer. The page MUST NOT implement meeting-room or video-meeting booking UI. The sidebar MUST list calendars shared to the user. Day/week views MUST support drag-to-reschedule for organizers.

#### Scenario: View switch

- **WHEN** a user switches among day, week, and month
- **THEN** the main grid updates without leaving `/app/calendar`

#### Scenario: Quick create

- **WHEN** a user creates an event from the quick-create dialog
- **THEN** the event appears on the grid after save (via store/API)
- **AND** color follows the selected calendar

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
