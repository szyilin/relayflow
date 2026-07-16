## ADDED Requirements

### Requirement: Notify type catalog

The system SHALL treat notification `type` as an extensible string catalog. Core constants MUST include at least `MEMBER_INVITE`, `TASK_DUE`, `TASK_ASSIGNED`, `IM_MENTION`, and `APPROVAL_PENDING`. Push MUST NOT reject unknown types solely because they are not in the core catalog.

#### Scenario: Known type stored

- **WHEN** a module pushes a notification with `type=TASK_DUE`
- **THEN** the system persists the row with that type
- **AND** inbox APIs return the same type to the client

#### Scenario: Unknown type still accepted

- **WHEN** a module pushes a notification with a non-catalog type string
- **THEN** the system still persists and returns the notification
- **AND** does not fail the push solely due to unknown type

### Requirement: Notify dedupe key

The system SHALL support an optional `dedupeKey` on `NotifyInboxApi.push`. When `dedupeKey` is present, an unread notification for the same `tenant_id`, receiver (`user_id` or `mobile`), `type`, and `dedupe_key` MUST be updated in place instead of inserting a duplicate unread row.

#### Scenario: Idempotent task due reminder

- **WHEN** task-biz pushes `TASK_DUE` twice with the same `dedupeKey=task:{taskId}` for the same assignee while the first remains unread
- **THEN** only one unread inbox row exists for that key
- **AND** title/body/payload are refreshed from the latest push

#### Scenario: Null dedupe keeps V1 invite behavior

- **WHEN** system-biz pushes `MEMBER_INVITE` without a `dedupeKey`
- **THEN** the system continues to idempotently refresh by tenant + receiver + type for unread rows (existing invite behavior)

### Requirement: Notify payload deep link convention

The system SHALL allow `payload_json` to include optional `route` (in-app path), `entityType`, and `entityId` so workspace clients can navigate to the related context.

#### Scenario: Task due payload shape

- **WHEN** a `TASK_DUE` notification is created
- **THEN** its payload includes `entityType` of `task`, `entityId` of the task id, and a `route` starting with `/app/tasks`

### Requirement: Mark all notifications read

The system SHALL expose `POST /app-api/infra/notify/read-all` for the authenticated user to mark all of their notifications read, optionally filtered by `type`.

#### Scenario: Read all

- **WHEN** an authenticated user posts `POST /app-api/infra/notify/read-all` without a type filter
- **THEN** all unread notifications for that `user_id` become read
- **AND** subsequent unread-count is zero for that user

#### Scenario: Read all of one type

- **WHEN** an authenticated user posts `POST /app-api/infra/notify/read-all` with `type=TASK_DUE`
- **THEN** only unread `TASK_DUE` rows for that user become read
- **AND** other types remain unread if they were unread

### Requirement: Filter inbox by type

The system SHALL support optional `type` query parameter on `GET /app-api/infra/notify/page` to return only notifications of that type for the current user.

#### Scenario: Filter task notifications

- **WHEN** an authenticated user requests `GET /app-api/infra/notify/page?type=TASK_DUE`
- **THEN** every returned item has `type=TASK_DUE`
- **AND** no other user's notifications are returned

## MODIFIED Requirements

### Requirement: Notify WebSocket domain (optional V1)

The system MUST deliver `domain=notify` WebSocket envelopes when a notification is pushed to an online user with a non-null `user_id`. Unread state MUST still be available via REST for offline users and as a fallback.

#### Scenario: Online user receives notify envelope

- **WHEN** a notification is pushed to a user who has an active WebSocket session and a resolved `user_id`
- **THEN** the client receives `{ domain: "notify", type: "notify.new", payload }` including at least `unreadCount`
- **AND** the delivery path uses `RealtimeEventPublisher` / realtime transport (not a direct Session import from business modules)

#### Scenario: Mobile-only invite without user id

- **WHEN** a `MEMBER_INVITE` is pushed with null `user_id` and only `mobile`
- **THEN** the system MUST NOT require WebSocket delivery
- **AND** the notification remains available after user-id backfill via REST inbox APIs

#### Scenario: Offline user relies on REST

- **WHEN** a notification is pushed to a user without an active WebSocket session
- **THEN** the next authenticated `GET /app-api/infra/notify/unread-count` or page request reflects the new unread state
