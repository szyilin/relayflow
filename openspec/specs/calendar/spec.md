# calendar Specification

## Purpose

工作台日历域：日历容器、日程、租户内邀约、Bot 提醒触达，以及 `/app/calendar` 日/周/月视图行为。

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

The system SHALL allow organizers to create, update, and soft-delete events on calendars they own. Events MUST belong to a `calendar_id`, store `start_time`/`end_time` as timestamptz, and support `all_day`. V1 MUST NOT implement recurring (RRULE) events.

#### Scenario: Create timed event

- **WHEN** a member creates an event on an owned calendar with title and start/end
- **THEN** the event is persisted with `organizer_id` equal to the current user
- **AND** the organizer is recorded as an attendee with role ORGANIZER

#### Scenario: Forbidden edit by non-organizer

- **WHEN** an attendee who is not the organizer attempts to change title or time
- **THEN** the system rejects with `EVENT_FORBIDDEN` (or equivalent)

### Requirement: Event list visibility

For a time range query, the system SHALL return events that are either (1) on calendars owned by the current user, or (2) events where the current user is an attendee (invited), within the current tenant.

#### Scenario: Invitee sees event without owning calendar

- **WHEN** user B is an attendee of an event on user A's calendar
- **AND** B requests event list covering that time range
- **THEN** the event is included in B's result
- **AND** B is not required to own A's calendar

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

The `/app/calendar` page SHALL provide day/week/month views, a sidebar with mini-month and owned calendars (checkbox + color), quick-create event UI, and Rail navigation entry. V1 MUST NOT show a「我的任务」virtual calendar layer. V1 MUST NOT implement meeting-room or video-meeting booking UI.

#### Scenario: View switch

- **WHEN** a user switches among day, week, and month
- **THEN** the main grid updates without leaving `/app/calendar`

#### Scenario: Quick create

- **WHEN** a user creates an event from the quick-create dialog
- **THEN** the event appears on the grid after save (via store/API)
- **AND** color follows the selected calendar

### Requirement: No whole-calendar share in V1

V1 MUST NOT implement whole-calendar ACL, subscribe-to-colleague calendar, or free/busy-only sharing. Those belong to a later change (`workspace-calendar-v1.1`).

#### Scenario: No subscribe API

- **WHEN** a client calls a calendar subscribe/share endpoint that is out of V1
- **THEN** such endpoints are absent or rejected as not implemented
