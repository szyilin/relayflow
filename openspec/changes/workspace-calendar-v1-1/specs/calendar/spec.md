## REMOVED Requirements

### Requirement: No whole-calendar share in V1

V1 MUST NOT implement whole-calendar ACL, subscribe-to-colleague calendar, or free/busy-only sharing. Those belong to a later change (`workspace-calendar-v1.1`).

#### Scenario: No subscribe API

- **WHEN** a client calls a calendar subscribe/share endpoint that is out of V1
- **THEN** such endpoints are absent or rejected as not implemented

## MODIFIED Requirements

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

### Requirement: Workspace calendar page

The `/app/calendar` page SHALL provide day/week/month views, a sidebar with mini-month and owned calendars (checkbox + color), quick-create event UI, and Rail navigation entry. The page MUST NOT show a「我的任务」virtual calendar layer. The page MUST NOT implement meeting-room or video-meeting booking UI. After share ships, the sidebar MUST list calendars shared to the user. After DnD ships, day/week views MUST support drag-to-reschedule for organizers.

#### Scenario: View switch

- **WHEN** a user switches among day, week, and month
- **THEN** the main grid updates without leaving `/app/calendar`

#### Scenario: Quick create

- **WHEN** a user creates an event from the quick-create dialog
- **THEN** the event appears on the grid after save (via store/API)
- **AND** color follows the selected calendar

## ADDED Requirements

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
