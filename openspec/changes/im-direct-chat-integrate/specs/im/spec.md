## MODIFIED Requirements

### Requirement: App direct chat integration

The `/app/messages` workspace SHALL load conversations and messages from `/app-api/im/*` without Mock fallback when the backend is available.

#### Scenario: REST conversation list

- **WHEN** an authenticated user opens `/app/messages`
- **THEN** the client SHALL request `GET /app-api/im/conversation/list` and render the response

#### Scenario: Realtime message push

- **WHEN** another member sends a message in a shared direct conversation
- **THEN** the client SHALL receive WebSocket envelope `domain=im`, `type=message.new` and update the UI

#### Scenario: Send without Mock

- **WHEN** the user sends a text message
- **THEN** the client SHALL call `POST /app-api/im/message/send` with `clientMsgId` and SHALL NOT fall back to local Mock data
