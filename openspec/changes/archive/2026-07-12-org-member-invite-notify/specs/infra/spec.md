## ADDED Requirements

### Requirement: Notify inbox persistence

The system SHALL persist in-app notifications in `infra_notify` with tenant isolation, receiver identity (`user_id` and/or `mobile`), type, title, body, optional JSON payload, and read state.

#### Scenario: Store member invite notification

- **WHEN** an admin successfully invites a mobile number to the current tenant
- **THEN** the system inserts or updates an `infra_notify` row with `type=MEMBER_INVITE`
- **AND** sets `tenant_id` to the inviting tenant
- **AND** sets `user_id` when a global user already exists for that mobile, otherwise leaves `user_id` null and sets `mobile`

#### Scenario: Backfill user id after registration

- **WHEN** a user registers or sets password for a mobile that has pending `infra_notify` rows with null `user_id`
- **THEN** the system associates those rows with the new `sys_user.id`

### Requirement: NotifyInboxApi cross-module push

The system SHALL expose `NotifyInboxApi` in `relayflow-module-infra-api` and implement it in `infra-biz` so other modules can push notifications without depending on `infra-biz` implementation types.

#### Scenario: System module pushes invite notification

- **WHEN** `system-biz` completes `inviteMember`
- **THEN** it calls `NotifyInboxApi.push` with `type=MEMBER_INVITE`
- **AND** does not access `infra_notify` Mapper directly from `system-biz`

### Requirement: Workspace notify inbox APIs

The system SHALL expose authenticated app APIs for the current user to list notifications, read notifications, and query unread count.

#### Scenario: Paginated inbox

- **WHEN** an authenticated user requests `GET /app-api/infra/notify/page`
- **THEN** the system returns notifications where `user_id` equals the JWT user
- **AND** does not return notifications belonging to other users

#### Scenario: Mark notifications read

- **WHEN** an authenticated user posts `POST /app-api/infra/notify/read` with notification ids owned by that user
- **THEN** the system sets `read_flag=1` for those rows

#### Scenario: Unread count for badge

- **WHEN** an authenticated user requests `GET /app-api/infra/notify/unread-count`
- **THEN** the system returns the count of unread notifications for that user

### Requirement: Notify WebSocket domain (optional V1)

The system MAY deliver `domain=notify` WebSocket envelopes when a notification is pushed to an online user; if not implemented in V1, unread state MUST still be available via REST.

#### Scenario: Online user receives notify envelope

- **WHEN** a notification is pushed to a user who has an active WebSocket session
- **THEN** the client MAY receive `{ domain: "notify", type: "notify.new", payload: { unreadCount } }`
