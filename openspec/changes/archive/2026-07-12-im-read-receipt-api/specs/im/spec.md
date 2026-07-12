## ADDED Requirements

### Requirement: Conversation read status query

The system SHALL expose member read sequences for a conversation so clients can render read receipts.

#### Scenario: Query read status as member

- **WHEN** an authenticated member requests read status for a conversation they belong to
- **THEN** the API returns each member's userId and readSeq

#### Scenario: Reject non-member read status

- **WHEN** the user is not a conversation member
- **THEN** the API rejects the request

### Requirement: Read update realtime event

When a member marks a conversation read, the system SHALL notify other online members via WebSocket.

#### Scenario: Fanout read updated

- **WHEN** a member successfully updates readSeq for a conversation
- **THEN** other conversation members receive an `im` domain `read.updated` event with userId and readSeq
