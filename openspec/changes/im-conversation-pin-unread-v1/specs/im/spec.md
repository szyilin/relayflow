## ADDED Requirements

### Requirement: Windowed message list by seq

系统 MUST 在用户端消息列表 API 支持按会话 seq 的窗口读取：至少支持最新窗口、`afterSeq` 向后、`beforeSeq` 向前，以及围绕 `aroundSeq` 的上下窗口；每窗条数 MUST 有服务端硬上限。既有仅 `afterSeq` 的调用 MUST 保持兼容或提供等价迁移路径。

#### Scenario: Fetch around first unread

- **WHEN** 成员请求某会话 `aroundSeq=N` 且指定 before/after 上限内条数
- **THEN** 返回该会话中 N 附近窗口内的消息（按 seq 有序），不超过硬上限

#### Scenario: Fetch history before cursor

- **WHEN** 成员请求 `beforeSeq=N` 与 limit
- **THEN** 返回 seq 严格小于 N 的至多 limit 条消息

### Requirement: Conversation list exposes pin and inbox fields

会话列表 API MUST 向当前用户返回置顶态（及排序所需时间）、旗标态、免打扰态（`muted` 或 `muteUntil`），以及由 inbox 水位派生的 `unreadCount`；真实已读水位 MUST 可被客户端获取（列表字段或既有 read-status），以便区分回执与红点。

#### Scenario: List item includes pin flag mute and unread

- **WHEN** 成员拉取会话列表
- **THEN** 每条含 pinned（及 pinnedAt 或等价）、flagged、muted（或 muteUntil）与 unreadCount

## MODIFIED Requirements

### Requirement: Conversation message cache and scroll UX

The workspace IM message panel MUST avoid full-panel skeleton flicker when revisiting a conversation that already has in-memory messages, MUST open with windowed messages appropriate to unread state (jump to first unread when inbox unread exists; otherwise latest window), and MUST expose unread / back-to-latest navigation similar to mainstream IM clients.

#### Scenario: Cached conversation switch without skeleton flash

- **WHEN** the user selects a conversation that already has cached messages in memory
- **THEN** the UI shows cached messages immediately without replacing the list with a full skeleton
- **AND** the client still requests the message list from the API in the background and merges updates

#### Scenario: Open conversation scrolls to bottom

- **WHEN** the user opens a conversation with no pending unread jump target
- **THEN** after messages render, the viewport MUST be scrolled to the latest (bottom) messages
- **AND** this MUST work even when the previous conversation had the same message count

#### Scenario: Jump to unread and back to latest

- **WHEN** a conversation has inbox unread messages when opened
- **THEN** the UI MUST jump to the first unread region and show a new-message divider
- **AND** when the user scrolls away from the bottom, the UI MUST show a control to return to the latest messages
- **AND** returning to latest MUST load the latest seq window without fetching the entire gap

#### Scenario: Read watermark on enter

- **WHEN** the user enters an active conversation message panel
- **THEN** the client MUST report true read progress to the conversation max seq for that enter (advancing peer-visible `read_seq` and clearing inbox badge), and MUST NOT require every unread bubble to have been visible in the viewport
- **AND** while the conversation remains active, the client MUST continue to advance watermarks for newly arrived messages
- **AND** correctness MUST NOT depend on a mandatory leave-time flush

#### Scenario: Open with no unread stays at latest

- **WHEN** the user opens a conversation with no inbox unread jump target
- **THEN** after messages render, the viewport MUST be scrolled to the latest (bottom) messages
