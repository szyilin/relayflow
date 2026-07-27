## ADDED Requirements

### Requirement: Conversation message cache and scroll UX

The workspace IM message panel MUST avoid full-panel skeleton flicker when revisiting a conversation that already has in-memory messages, MUST scroll to the latest messages when opening a conversation (unless jumping to unread), and MUST expose unread / back-to-latest navigation similar to mainstream IM clients.

#### Scenario: Cached conversation switch without skeleton flash

- **WHEN** the user selects a conversation that already has cached messages in memory
- **THEN** the UI shows cached messages immediately without replacing the list with a full skeleton
- **AND** the client still requests the message list from the API in the background and merges updates

#### Scenario: Open conversation scrolls to bottom

- **WHEN** the user opens a conversation with no pending unread jump target
- **THEN** after messages render, the viewport MUST be scrolled to the latest (bottom) messages
- **AND** this MUST work even when the previous conversation had the same message count

#### Scenario: Jump to unread and back to latest

- **WHEN** a conversation has unread messages when opened
- **THEN** the UI MAY show a control to jump to the first unread / latest unread region
- **AND** when the user scrolls away from the bottom, the UI MUST show a control to return to the latest messages

#### Scenario: Read watermark on view / leave

- **WHEN** the user views messages in an active conversation
- **THEN** the client MUST report read progress based on visible messages (or on leaving the conversation), not solely by marking the max seq immediately on open with no regard to viewport
