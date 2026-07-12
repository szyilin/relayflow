## ADDED Requirements

### Requirement: Direct chat read receipt UI

The workspace `/app/messages` page SHALL display a read indicator on the sender's own messages in direct conversations when the peer member's read sequence is greater than or equal to the message sequence.

#### Scenario: Show read label on direct messages

- **WHEN** the user views a direct conversation they participated in as sender
- **AND** the peer member's readSeq is at least the message seq
- **THEN** the UI shows a read receipt label on that outgoing message bubble

#### Scenario: Hide read label in group chats

- **WHEN** the active conversation type is group
- **THEN** per-message read receipts are not shown in V1

#### Scenario: Read status mock fallback

- **WHEN** read-status API is not yet available
- **THEN** the store MAY simulate peer read state for UI demonstration without page-level mock imports
