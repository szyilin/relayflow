# workspace-agent-chat-stream Specification

## Purpose
TBD - created by archiving change workspace-agent-chat-stream-api. Update Purpose after archive.
## Requirements
### Requirement: Stream send endpoint

The system SHALL expose `POST /app-api/agent/session/send/stream` that authenticates the current user, verifies session ownership, and responds with `Content-Type: text/event-stream` whose `data:` lines are UTF-8 JSON events as defined by `openspec/lanes/workspace-agent-chat/contract.md`.

#### Scenario: Successful stream with text

- **WHEN** an authenticated owner of a session posts valid `{ sessionId, content }` to `/send/stream`
- **THEN** the server emits `message.user`, zero or more `text-delta` events, a final `message.assistant` with `content` and `parts`, then `done`

#### Scenario: Unauthorized session

- **WHEN** the session does not belong to the current user
- **THEN** the request fails with the same ownership error semantics as sync `/send` (no assistant persistence)

### Requirement: Persist message parts

The system SHALL store assistant message trajectory in `agent_session_message.parts_json` when available, while keeping `content` as the concatenated text for compatibility. User messages MAY store a single text part.

#### Scenario: History returns parts

- **WHEN** a client calls `GET /app-api/agent/session/messages?sessionId=`
- **THEN** each message MAY include `parts` parsed from `parts_json`; when `parts_json` is null, `parts` MAY be omitted and clients MUST treat `content` as a single text part

### Requirement: Sync send remains available

The system SHALL keep `POST /app-api/agent/session/send` working for non-streaming clients.

#### Scenario: Sync send still returns both messages

- **WHEN** a client calls sync `/send`
- **THEN** the response still includes `userMessage` and `assistantMessage` with `content` (and MAY include `parts` if populated)

### Requirement: Client abort keeps user message

When the client aborts the stream after the user message was persisted, the system MUST retain that user message and MUST NOT require an assistant row for that turn.

#### Scenario: Abort mid-generation

- **WHEN** the client disconnects during generation after `message.user` was emitted
- **THEN** the user message remains in the session history and no complete assistant message is required for that aborted turn

