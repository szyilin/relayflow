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

### Requirement: Stream reasoning when Spring AI provides it

`SpringAiAgentRuntime` MUST 使用 ChatResponse 流（非仅 `.content()`），并在 AssistantMessage metadata 存在非空 `reasoningContent` 时发出 `reasoning-delta`（及适当时 `reasoning-end`）。当模型/网关不提供 reasoning 时 MUST 仅发 text-delta，MUST NOT 为各厂商私有协议单独硬编码适配层。

#### Scenario: Compatible reasoning model

- **WHEN** OpenAI 兼容服务在流式块中返回 `reasoning_content`
- **THEN** 客户端收到 `reasoning-delta` 事件并可渲染 `UChatReasoning`

#### Scenario: Plain chat model

- **WHEN** 模型不返回 reasoningContent
- **THEN** 流仅含 text-delta（及既有 tool 事件若有），行为与先前一致
