## ADDED Requirements

### Requirement: Group chat integration without mock

The workspace IM store SHALL use real group chat REST APIs after backend implementation, without mock fallback for group create, invite, or member list.

#### Scenario: Create group against real API

- **WHEN** the user creates a group with backend running
- **THEN** the store calls `POST /app-api/im/group/create`
- **AND** does not fall back to `mocks/im.ts`

#### Scenario: Group messages over REST and WebSocket

- **WHEN** the user sends or receives group messages with backend running
- **THEN** messages are loaded and sent via existing IM REST/WS endpoints
- **AND** system join messages appear in the thread
