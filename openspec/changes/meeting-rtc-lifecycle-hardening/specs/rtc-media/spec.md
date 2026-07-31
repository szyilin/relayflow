## ADDED Requirements

### Requirement: End session closes LiveKit room

When a media session is ended via `RtcSessionApi.endSession` (including when meeting-biz ends a meeting that owns the session), rtc-biz MUST attempt to close the corresponding LiveKit room through the LiveKit server Room API (delete room and/or remove all participants) using server-side API credentials. rtc-biz MUST then persist `rtc_session` status as ended. If the Room API call fails, rtc-biz MUST still mark the session ended and MUST log the failure; it MUST NOT leave the session ACTIVE solely because media teardown failed. After end, `issueJoinToken` for that session MUST fail.

#### Scenario: Meeting end tears down media room

- **WHEN** meeting-biz ends a live meeting that has an associated rtc session
- **THEN** rtc attempts LiveKit room teardown for that session's `room_name`
- **AND** `rtc_session.status` becomes ended
- **AND** subsequent `issueJoinToken` for that session id is rejected

#### Scenario: Room API failure still ends DB session

- **WHEN** LiveKit Room API is unreachable during endSession
- **THEN** rtc still marks the session ended in the database
- **AND** logs a warning suitable for operators

### Requirement: Idempotent active session per owner

For a given tenant, `owner_type`, `owner_id`, and `purpose`, rtc MUST ensure at most one `ACTIVE` (non-deleted) `rtc_session` row. Concurrent `ensureSession` calls MUST return the same active session rather than inserting duplicates. The database MUST enforce this with a partial unique constraint or equivalent.

#### Scenario: Concurrent ensure

- **WHEN** two callers ensure a session for the same owner and purpose concurrently
- **THEN** only one ACTIVE row remains
- **AND** both callers receive that session id

## MODIFIED Requirements

### Requirement: LiveKit webhook ingestion

The rtc domain SHALL accept LiveKit webhooks authenticated with a shared secret Bearer token and/or LiveKit webhook signature verification, without requiring end-user JWT. Handlers MUST be idempotent. Webhooks MAY update `rtc_*` state only and MUST NOT write `mtg_*` or other product tables. Participant count updates MUST be applied with an atomic SQL increment/decrement bounded at zero. Cross-domain side effects (if any) MUST use domain events after local commit; V1 MAY omit product consumers. Deployment documentation MUST describe how to point LiveKit at `POST /app-api/rtc/webhook` for local or self-hosted wiring (including container-to-host networking caveats).

#### Scenario: Participant joined webhook

- **WHEN** LiveKit delivers a verified participant-joined webhook for a known active room
- **THEN** rtc atomically increments `rtc_session.participant_count` without failing acknowledgement on benign duplicates
- **AND** does not write meeting-domain tables directly

#### Scenario: Participant left webhook

- **WHEN** LiveKit delivers a verified participant-left webhook for a known active room
- **THEN** rtc atomically decrements `participant_count` with a floor of zero

#### Scenario: Operators can wire webhook

- **WHEN** an operator reads RTC/LiveKit deployment docs
- **THEN** they can find the webhook path, auth header expectation, and an example of configuring LiveKit to deliver events to RelayFlow
