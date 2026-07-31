# Contract draft — im-conversation-pin-unread-v1

> `-web` 起草后，`-api` / `-integrate` 已对齐：列表字段、成员态动作、标签、偏好、消息窗口均走真实 app-api。

## Conversation list item extras

| Field | Type | Notes |
|-------|------|-------|
| `pinned` | boolean | member-private |
| `pinnedAt` | string \| null | ISO |
| `flagged` | boolean | member-private |
| `muted` | boolean | or derive from `muteUntil > now` |
| `muteUntil` | string \| null | optional raw |
| `unreadCount` | number | inbox badge (later from `badge_seq`) |
| `mentionedMe` | boolean | existing |
| `tagIds` | string[] | current user's assigned tag ids (private) |

## Actions (planned)

| Method | Path | Body | Notes |
|--------|------|------|-------|
| POST | `/app-api/im/conversation/pin` | `{ conversationId, pinned }` | |
| POST | `/app-api/im/conversation/flag` | `{ conversationId, flagged }` | |
| POST | `/app-api/im/conversation/mute` | `{ conversationId, muted }` or `muteUntil` | |
| POST | `/app-api/im/conversation/inbox/mark-read` | `{ conversationId }` | advances `badge_seq` only → unread 0 |
| POST | `/app-api/im/conversation/inbox/mark-unread` | `{ conversationId }` | sets inbox so badge shows **exactly 1**; MUST NOT change `read_seq` |
| GET | `/app-api/im/tag/list` | — | current user tag library (+ `conversationCount`) |
| POST | `/app-api/im/tag/create` | `{ name }` | create in library |
| POST | `/app-api/im/tag/assign` | `{ conversationId, tagId }` | idempotent assign |
| POST | `/app-api/im/tag/unassign` | `{ conversationId, tagId }` | remove from one conversation |
| POST | `/app-api/im/tag/delete` | `{ tagId }` | delete library tag + all assignments |
| POST | `/app-api/im/tag/create-and-assign` | `{ conversationId, name }` | optional merge |

## Preference `settings.im`

| Key | Type | Default |
|-----|------|---------|
| `pinnedSectionCollapsed` | boolean | false |
| `chatBubbleLayout` | existing | |
| `sidebarGroupOrder` | string[] | `['all','unread','flagged','mention_me','tags','direct','group','muted','bot']` |
| `sidebarGroupHidden` | string[] | `[]` |

`sidebarGroupOrder` = visible built-in ids in display order. Unknown ids discarded; empty visible list merges back to full default. User tag children are **not** preference entries — they hang under `tags` from the tag library.

## Sidebar groups (client filter)

| id | Label | Filter |
|----|-------|--------|
| `all` | 消息 | all conversations |
| `unread` | 未读 | `unreadCount > 0` |
| `flagged` | 标记 | `flagged` |
| `mention_me` | @我 | unread AND `mentionedMe` |
| `tags` | 标签 | parent expand only; select concrete `tag:{id}` |
| `direct` / `group` / `muted` / `bot` | … | type / mute / `bot_dm` |

（分组栏见上。）

## Message list window (`GET /app-api/im/message/list`)

| Param | Type | Notes |
|-------|------|-------|
| `conversationId` | string | required |
| `afterSeq` | number | forward: seq > afterSeq |
| `beforeSeq` | number | history: seq < beforeSeq |
| `aroundSeq` | number | anchor for unread jump (**includes** message at aroundSeq) |
| `before` / `after` | number | counts around `aroundSeq` (defaults 25/25) |
| `limit` | number | max items; **server hard-cap 50** (default 50) |

Modes (priority: around → before → after → latest):

1. **Latest window** — no after/before/around (or afterSeq≤0) → highest seq side, `limit`
2. **Around unread** — `aroundSeq` + `before` + `after` (total capped at 50)
3. **Older** — `beforeSeq` + `limit`
4. **Newer** — `afterSeq` > 0 + `limit`

**Unread materialization:** on send, `unread_count = max(0, maxSeq - badge_seq)`.  
**True read** (`POST …/conversation/read`): advances `read_seq` + `badge_seq`, fans out `read.updated` when `read_seq` rises.
