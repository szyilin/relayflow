# im-message-reaction Specification

## Purpose

Message emoji reactions, aggregates, realtime sync, and UI entry points.

## Requirements

### Requirement: Toggle message reaction

The system SHALL allow a conversation member to add or remove a Unicode emoji reaction on a non-recalled message via `POST /app-api/im/message/reaction/toggle` with `{ messageId, emoji }`. Repeating the same emoji for the same user MUST remove it (toggle). The server MUST NFC-normalize emoji and reject empty or overly long values. Each user MUST NOT exceed 20 distinct emoji on one message.

#### Scenario: Add reaction

- **WHEN** a member toggles an emoji not yet set by them on a visible message
- **THEN** the system persists the reaction
- **AND** returns the updated aggregate list for that message

#### Scenario: Remove reaction by toggle

- **WHEN** the same member toggles the same emoji again
- **THEN** the system deletes that reaction row
- **AND** returns the updated aggregate

#### Scenario: Recalled message rejects new reaction

- **WHEN** the target message is recalled
- **THEN** the system MUST reject the toggle with a client-visible error
- **AND** MUST NOT insert a reaction

### Requirement: Reaction aggregate on messages

Message list responses and `message.new` payloads MUST include `reactions` as an array of `{ emoji, count, me }` aggregated for the current viewer (`me` true if the current user reacted with that emoji). Ordering SHOULD prefer higher count then emoji code order.

#### Scenario: List includes aggregates

- **WHEN** a member loads `/app-api/im/message/list`
- **THEN** each message includes `reactions` (empty array if none)

### Requirement: Reaction realtime sync

After a successful toggle, the system MUST push `domain=im, type=reaction.updated` to online User members of the conversation with `{ conversationId, messageId, reactions }` as a full aggregate snapshot for that message. Clients MUST replace local aggregates for that message id.

#### Scenario: Peer sees reaction update

- **WHEN** user A toggles a reaction
- **THEN** online user B in the same conversation receives `reaction.updated`
- **AND** B's UI shows the new aggregate without a full message reload

### Requirement: Reaction UI entry points

The workspace message UI MUST expose reaction entry via the message context menu and a quick emoji bar (at least six common emoji plus a more picker). Existing reactions MUST render under the bubble as tappable chips that toggle the current user's participation.

#### Scenario: Quick bar adds reaction

- **WHEN** the member picks an emoji from the quick bar on a message
- **THEN** the client calls toggle and updates the chip row from the response or WS event
