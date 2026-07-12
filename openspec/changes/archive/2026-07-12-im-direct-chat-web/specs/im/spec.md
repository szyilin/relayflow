## ADDED Requirements

### Requirement: Workspace direct chat UI

The workspace `/app/messages` page SHALL display a conversation list, message thread, and text input for direct chats, using Pinia store and `/app-api/im/` contracts.

#### Scenario: Conversation list with mock fallback

- **WHEN** the user opens `/app/messages` and IM REST APIs are unavailable
- **THEN** the page displays mock conversations from the store fallback
- **AND** the page MUST NOT import mocks directly

#### Scenario: Send text message optimistically

- **WHEN** the user sends a text message in an active conversation
- **THEN** the UI shows the message immediately with a sending state
- **AND** includes `clientMsgId` for idempotent retry semantics in the API contract
