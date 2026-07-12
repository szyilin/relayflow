## ADDED Requirements

### Requirement: Direct chat REST APIs

The system SHALL expose `/app-api/im/conversation/list`, `/app-api/im/message/list`, and `/app-api/im/message/send` for authenticated workspace members per the im-direct-chat contract.

#### Scenario: List conversations for member

- **WHEN** an authenticated user requests `GET /app-api/im/conversation/list`
- **THEN** the system returns direct conversations where the user is a member
- **AND** each item includes peer display name and unread count

#### Scenario: Send message with persist-then-ack

- **WHEN** an authenticated member posts `POST /app-api/im/message/send` with valid text content
- **THEN** the message is persisted before the HTTP response
- **AND** the response includes server `id`, `seq`, and `clientMsgId`

#### Scenario: Idempotent client_msg_id

- **WHEN** the same `clientMsgId` is submitted twice by the same tenant
- **THEN** the system returns the original message identifiers
- **AND** does not insert a duplicate row

### Requirement: IM WebSocket domain handler

The `im-biz` module SHALL register a `RealtimeDomainMessageHandler` for domain `im` that handles upstream `message.send`.

#### Scenario: WebSocket send ack after commit

- **WHEN** a client sends envelope `domain=im`, `type=message.send` with valid payload
- **THEN** the system persists the message
- **AND** sends `message.ack` to the sender session after DB commit
- **AND** fans out `message.new` to other online conversation members
