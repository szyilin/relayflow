## ADDED Requirements

### Requirement: IM deepening V1 roadmap

The product SHALL deliver group chat, file/image messages, read receipt UI, and online presence as independent vertical slices under the `im-deepening-v1` parent change, each following `-web` → `-api` → `-integrate` order.

#### Scenario: Slice execution order

- **WHEN** implementing IM deepening after direct chat
- **THEN** the team completes `im-group-chat-*`, then `im-message-file-*`, then `im-read-receipt-*`, then `im-presence-*`
- **AND** each slice has a permanent contract under `openspec/lanes/`

### Requirement: File and image chat messages

The IM module SHALL accept `type=image|file` messages whose content blocks reference tenant-scoped infra files, and list responses SHALL include download URLs for file blocks.

#### Scenario: Send image message

- **WHEN** a member sends an image via `/app-api/im/message/send` with a valid private `fileId`
- **THEN** the message persists with `type=image`
- **AND** `lastMsgPreview` is `[图片]`

#### Scenario: List message download URL

- **WHEN** a member lists messages containing file blocks
- **THEN** each file block includes `downloadUrl` pointing to `/app-api/infra/file/download`

### Requirement: Read receipt visibility

The workspace SHALL expose peer read watermarks for direct conversations and push `read.updated` over WebSocket when a member marks a conversation read.

#### Scenario: Query read status

- **WHEN** a member requests `/app-api/im/conversation/read-status`
- **THEN** the response lists member `readSeq` values for that conversation

#### Scenario: Read updated fanout

- **WHEN** a member successfully calls `/app-api/im/conversation/read` with a higher `readSeq`
- **THEN** other online members receive `domain=im, type=read.updated`

### Requirement: Online presence batch query

The IM module SHALL expose batch online status for tenant members based on active WebSocket sessions.

#### Scenario: Batch presence

- **WHEN** a member requests `/app-api/im/presence/batch?userIds=...`
- **THEN** the response returns `online=true|false` only for active tenant members in the request list
