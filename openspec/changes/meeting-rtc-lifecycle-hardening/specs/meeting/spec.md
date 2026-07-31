## ADDED Requirements

### Requirement: Reliable meeting leave and host end controls

The meeting client SHALL report leave whenever the user leaves the in-meeting surface for real (explicit hangup, SPA route navigation away from the room page, or tab/window unload), using best-effort delivery suitable for page teardown. The in-meeting UI MUST expose「结束会议」for the meeting host that calls the existing meeting end API and disconnects media; non-hosts MUST only see leave (or equivalent) and MUST NOT be offered host end. After a successful host end, the meeting status MUST become ended and further join-credentials MUST fail with a clear business error.

#### Scenario: SPA navigates away reports leave

- **WHEN** a participant is connected in the room page and navigates to another app route without using hangup
- **THEN** the client still invokes meeting leave (or keepalive/beacon equivalent) at least once
- **AND** the participant row records `left_at` (or equivalent) so empty-room auto-end can run

#### Scenario: Host ends meeting from room

- **WHEN** the host confirms「结束会议」in the room UI
- **THEN** the client calls meeting end, disconnects LiveKit, and returns to the meeting Hub
- **AND** other participants lose usable media for that room after server-side session end handling

### Requirement: Join credential refresh for long meetings

The client SHALL be able to obtain a fresh LiveKit join token before the previous token expires while remaining in the same meeting, without requiring the user to manually re-enter the Hub. Re-issuing credentials MUST reuse the existing meeting authorization rules and MUST NOT create a second meeting record.

#### Scenario: Token nearing expiry

- **WHEN** a participant remains in a live meeting and the join token approaches expiry
- **THEN** the client requests join-credentials again and refreshes the LiveKit connection or token per the client SDK
- **AND** media participation continues without creating a new `mtg_meeting` row

## MODIFIED Requirements

### Requirement: Schedule meeting with calendar link

The system SHALL allow scheduling a meeting with title and time range. On success, meeting-biz MUST call the cross-domain calendar API on `calendar-api` (`CalEventApi.createLinkedEvent` or equivalent) to create a linked calendar event, and MUST store returned `calendar_event_id` when available. The event MUST persist optional `meeting_id`. calendar-biz MUST NOT depend on meeting-api or meeting-biz. meeting-biz MUST NOT write `cal_*` tables via mappers. Workspace calendar editors that schedule a RelayFlow video meeting MUST bind the calendar event created (or returned) by this path and MUST NOT create a second calendar event for the same `meeting_id` in the same scheduling action.

#### Scenario: Schedule creates calendar event

- **WHEN** a member schedules a meeting with a valid time range via meeting schedule API
- **THEN** an `mtg_meeting` in scheduled status is persisted
- **AND** a calendar event is created via calendar-api with `meeting_id` set
- **AND** `mtg_meeting.calendar_event_id` is stored when the API returns an event id

#### Scenario: Calendar editor does not double-create

- **WHEN** the workspace calendar editor creates a RelayFlow video meeting as part of saving an event
- **THEN** it uses the schedule path such that only one calendar event is linked to that meeting for that action
- **AND** it does not insert a second `cal_event` with the same `meeting_id` solely because both schedule and editor each created one

#### Scenario: No illegal calendar coupling

- **WHEN** architects inspect module dependencies and SQL
- **THEN** `meeting-biz` may depend on `calendar-api` and `rtc-api`
- **AND** `calendar-biz` MUST NOT depend on `meeting-api` or `meeting-biz`
- **AND** meeting mappers MUST NOT access `cal_*` tables

### Requirement: Pre-join window

Before entering LiveKit, the client SHALL show a pre-join window: local preview, microphone mute/unmute with a device picker, camera on/off with a device picker, and speaker/output volume or device control that actually affects remote audio playback (or output device selection when supported). Background/effects MAY be optional and MUST NOT block joining if unsupported. Selected input device ids MUST be applied when publishing tracks in the room.

#### Scenario: Enter muted

- **WHEN** the user leaves the microphone muted in pre-join and enters the room
- **THEN** published audio remains muted or unpublished until the user unmutes in-room

#### Scenario: Device pickers and speaker control

- **WHEN** the user opens pre-join on a browser that exposes multiple audio/video inputs
- **THEN** the UI lists selectable microphone and camera devices
- **AND** changing speaker volume (or output device when supported) changes audible remote playback after join, not only a unused UI state
