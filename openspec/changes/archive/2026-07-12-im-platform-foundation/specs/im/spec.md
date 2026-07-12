## ADDED Requirements

### Requirement: Unified conversation model

The system MUST use a unified `im_conversation` model for direct, group, and channel chats, distinguished by `type`; parallel table structures per chat type are forbidden.

#### Scenario: Direct conversation type

- **WHEN** the system creates or resolves a two-user conversation within a tenant
- **THEN** `im_conversation.type` is `direct`
- **AND** at most one direct conversation exists for the same user pair within the tenant

#### Scenario: Group conversation type

- **WHEN** the system creates a group chat with multiple tenant users
- **THEN** `im_conversation.type` is `group`
- **AND** membership is stored in `im_conversation_member`

#### Scenario: Channel conversation type

- **WHEN** the system creates a channel with subscribers
- **THEN** `im_conversation.type` is `channel`
- **AND** subscribers have role `subscriber`

### Requirement: Conversation member read watermark

The system MUST maintain `read_seq` and `unread_count` per conversation member for read state and unread badges.

#### Scenario: Report read progress

- **WHEN** an authenticated member reports read progress up to `seq=N` via WebSocket or REST
- **THEN** the system updates `read_seq` to `max(current, N)`
- **AND** recalculates `unread_count`

#### Scenario: Non-member denied

- **WHEN** a user who is not a conversation member requests messages or reports read state
- **THEN** the system rejects the request with a clear business error

### Requirement: Per-conversation message sequence

The system MUST assign a monotonically increasing `seq` per conversation message; client sync and read state MUST use `seq`, not client timestamps alone.

#### Scenario: Assign seq on send

- **WHEN** a user sends a new message to a conversation
- **THEN** the persisted message `seq` is greater than any existing message seq in that conversation

#### Scenario: Incremental fetch

- **WHEN** a member requests `/app-api/im/message/list?conversationId=&afterSeq=N`
- **THEN** the system returns messages with `seq > N` ordered ascending by `seq`

### Requirement: Client send idempotency

The system MUST support `client_msg_id` (client UUID); duplicate submissions with the same `client_msg_id` within a tenant MUST return the existing message without inserting a duplicate row.

#### Scenario: Duplicate client_msg_id

- **WHEN** a user resubmits a message with the same `client_msg_id` already accepted
- **THEN** the system returns the original server id and seq
- **AND** MUST NOT create a second message row

### Requirement: Message content blocks

The system MUST store message body as structured `content_json` (Content Block array); V1 MUST support `text` and `file` block types and reserve `link`, `actions`, and `mention`.

#### Scenario: Text message

- **WHEN** a user sends plain text
- **THEN** `content_json.blocks` contains at least one block with `type=text`

#### Scenario: File message references infra

- **WHEN** a user sends an attachment referencing a tenant-owned `fileId`
- **THEN** `content_json.blocks` contains `type=file` referencing `infra_file.fileId`
- **AND** the message API MUST NOT proxy multipart file bytes by default

### Requirement: Message publisher type

The system MUST record `sender_type` (`user` | `system` | `bot` | `app`); V1 MUST implement `user` and `system` and reserve the others.

#### Scenario: User send

- **WHEN** a normal user sends a message
- **THEN** `sender_type=user` and `sender_id` is the current user id

#### Scenario: System message

- **WHEN** a system event is sent via `ImMessageApi`
- **THEN** `sender_type=system`
- **AND** the message MUST NOT impersonate a normal user sender

### Requirement: Cross-module IM API

The system MUST expose cross-domain contracts in `relayflow-module-im-api` for conversation creation and system messages; other modules MUST NOT depend on `im-biz` implementations.

#### Scenario: Get or create direct chat

- **WHEN** module A calls `ImConversationApi.getOrCreateDirectConversation(userA, userB)`
- **THEN** a conversation id is returned
- **AND** module A MUST NOT access `im_*` mappers directly

#### Scenario: Send system message

- **WHEN** module A calls `ImMessageApi.sendSystemMessage(conversationId, content, publisher)`
- **THEN** the message is persisted to `im_message`
- **AND** online members receive WebSocket envelope `domain=im, type=message.new`

### Requirement: Realtime event publisher entry

The system MUST provide `RealtimeEventPublisher` in the platform layer for non-IM modules; V1 MAY no-op for `domain=notify` and `domain=presence`, but the interface and envelope `domain` enum MUST exist.

#### Scenario: Non-IM module publishes event

- **WHEN** `bpm-biz` publishes a `domain=notify` event via `RealtimeEventPublisher`
- **THEN** the call MUST NOT fail with missing interface errors
- **AND** V1 MAY use a documented no-op implementation

#### Scenario: IM must not bypass transport layer

- **WHEN** IM business logic pushes an envelope to users
- **THEN** it MUST use `RealtimeTransportApi`
- **AND** MUST NOT hold WebSocket Session references inside `im-biz`

### Requirement: Notify inbox API placeholder

The system MUST reserve `NotifyInboxApi` for future approval/task notifications; V1 MUST NOT require notify inbox tables or UI.

#### Scenario: V1 notify API call

- **WHEN** a module calls `NotifyInboxApi.push(...)` in V1
- **THEN** the system MAY throw unsupported or use a documented no-op
- **AND** MUST NOT persist notify content into `im_message`
