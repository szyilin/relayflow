# meeting Specification

## Purpose

工作台视频会议域：会议 Hub、即时/预约会议、会前预检与会中布局/侧边聊天，并通过 `RtcSessionApi` 与日历跨域 API 协作；不嵌入 LiveKit 密钥。

## Requirements

### Requirement: Meeting domain module

The system SHALL provide a `meeting` domain with Maven modules `relayflow-module-meeting-api` and `relayflow-module-meeting-biz`, persist meeting data in tables prefixed with `mtg_`, and load the biz module from `relayflow-server`. The meeting domain MUST obtain media rooms only via `RtcSessionApi` and MUST NOT embed LiveKit API secrets. Controllers under `/app-api/meeting/**` SHALL aggregate join credentials for the browser; the browser MUST NOT call a public rtc join-token API in V1.

#### Scenario: Table naming

- **WHEN** a new meeting-domain table is created
- **THEN** its name starts with `mtg_`
- **AND** it includes `tenant_id BIGINT NOT NULL`

### Requirement: Workspace meeting Hub

The workspace SHALL expose a「视频会议」entry that opens `/app/meeting` (or equivalent). The Hub MUST provide **发起会议**, **加入会议**, and **预约会议**. V1 MUST NOT require会议室投屏、妙记、智能纪要、直播, or 电话 to be functional.

#### Scenario: Open Hub

- **WHEN** a logged-in tenant member opens the meeting Hub
- **THEN** the three primary actions are available
- **AND** the page renders without requiring LiveKit connectivity until the user proceeds to start or join

### Requirement: Instant meeting start

The system SHALL allow a member to create an instant meeting record, complete pre-join, then obtain join credentials and enter a multi-party LiveKit room. Media session creation MUST be **lazy**: `rtc_session` is ensured on first join-credentials request, not necessarily at meeting insert time.

#### Scenario: Host starts meeting

- **WHEN** the host confirms「开始会议」from the pre-join UI
- **THEN** meeting ensures an rtc session, records participant join, and returns LiveKit URL + token
- **AND** the client connects to LiveKit and publishes tracks per pre-join mic/camera choices

### Requirement: Join meeting by code or id

The system SHALL allow a tenant member to join an existing non-ended meeting using `meeting_code` and/or meeting id / deep link. V1 MUST require an authenticated **effective member of the current tenant**. External/anonymous join is out of scope.

#### Scenario: Join with code

- **WHEN** a member submits a valid `meeting_code` for a non-ended meeting in the tenant
- **THEN** the system accepts join and proceeds to pre-join then credentials
- **AND** when the code is unknown or the meeting is ended, the system returns a clear business error

### Requirement: Schedule meeting with calendar link

The system SHALL allow scheduling a meeting with title and time range. On success, meeting-biz MUST call a **new** cross-domain calendar API on `calendar-api` (e.g. `CalEventApi`) to create a linked calendar event, and MUST store returned `calendar_event_id` when available. The event MUST persist optional `meeting_id`. calendar-biz MUST NOT depend on meeting-api or meeting-biz. meeting-biz MUST NOT write `cal_*` tables via mappers.

#### Scenario: Schedule creates calendar event

- **WHEN** a member schedules a meeting with a valid time range
- **THEN** an `mtg_meeting` in scheduled status is persisted
- **AND** a calendar event is created via calendar-api with `meeting_id` set

#### Scenario: No illegal calendar coupling

- **WHEN** architects inspect module dependencies and SQL
- **THEN** `meeting-biz` may depend on `calendar-api` and `rtc-api`
- **AND** `calendar-biz` MUST NOT depend on `meeting-api` or `meeting-biz`
- **AND** meeting mappers MUST NOT access `cal_*` tables

### Requirement: Pre-join window

Before entering LiveKit, the client SHALL show a pre-join window: local preview, microphone mute/unmute with device picker, camera on/off with device picker, and speaker/output volume or device control. Background/effects MAY be optional and MUST NOT block joining if unsupported.

#### Scenario: Enter muted

- **WHEN** the user leaves the microphone muted in pre-join and enters the room
- **THEN** published audio remains muted or unpublished until the user unmutes in-room

### Requirement: In-meeting participant grid

The client SHALL show a tiled participant layout. Each tile MUST show display name and mute indicator (lower-left or equally visible). Active speakers MUST sort toward the front using LiveKit client active-speaker signals. Soft display strategies for large rooms are allowed; the product MUST NOT invent an organization-wide hard headcount cap.

#### Scenario: Speaking participant moves forward

- **WHEN** a remote participant becomes an active speaker
- **THEN** their tile is ordered ahead of non-speaking participants locally
- **AND** name and mute state remain visible

### Requirement: In-meeting side chat

The in-meeting UI SHALL provide simple plain-text side chat via **LiveKit DataChannel** in V1. V1 MUST NOT require `mtg_chat_message` persistence, IM conversation ids, attachments, or read receipts.

#### Scenario: Send chat message

- **WHEN** a participant sends a text message in the side chat
- **THEN** other connected participants in the same LiveKit room receive it over the data channel
- **AND** V1 MUST NOT require a durable meeting chat row to exist

### Requirement: Participant roster ownership

Meeting-domain participant records (if persisted) MUST be updated on meeting join-credentials / leave / end paths. LiveKit webhooks MUST NOT be the sole writer of `mtg_*` participant rows.

#### Scenario: Join credentials records participant

- **WHEN** a member successfully obtains join credentials
- **THEN** meeting MAY upsert participant membership for that user
- **AND** that write happens in meeting-biz, not in rtc webhook handlers

### Requirement: Meeting history on Hub

The Hub MAY list recent meetings the user hosted or joined. V1 MUST NOT require recording playback or offline upload.

#### Scenario: History after ending

- **WHEN** a user participated in an ended meeting
- **THEN** Hub history MAY list it with basic metadata
- **AND** MUST NOT require a recording file

### Requirement: Frontend reusable RTC composables

LiveKit connection helpers MUST live under a shared path (e.g. `web/src/composables/rtc/`) reusable by future IM call UIs. Meeting pages MUST NOT be the only non-importable copy of connection logic.

#### Scenario: Shared connect helper

- **WHEN** the meeting room view connects to LiveKit
- **THEN** it uses shared RTC helpers rather than a one-off inline-only implementation
