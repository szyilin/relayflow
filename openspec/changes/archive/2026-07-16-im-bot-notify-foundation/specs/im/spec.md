## ADDED Requirements

### Requirement: Bot catalog and enablement

The system MUST maintain a platform-level bot catalog (not owned by a tenant) and record enablement at tenant and user layers. V1 system bots MUST use `mandatory` / `default_on` policies. When a user becomes an ACTIVE member of a tenant, the system MUST auto-write user enablement for system bots that are already tenant-enabled. Bots MUST NOT be persisted as `sys_user` rows.

#### Scenario: Auto user enable on join

- **GIVEN** tenant T has tenant-enabled system bot `task-bot` with `default_on` or `mandatory`
- **WHEN** user U becomes ACTIVE in T
- **THEN** the system writes user enablement `(T, U, task-bot)`
- **AND** MUST NOT force-create a `bot_dm` conversation immediately

#### Scenario: Bot is not a login user

- **GIVEN** platform seed inserts a bot definition
- **WHEN** querying `sys_user`
- **THEN** no login account row exists for that bot

### Requirement: bot_dm conversation

The system MUST support `im_conversation.type=bot_dm` for a one-to-one User↔Bot conversation in a tenant context. Conversations MUST be lazy-created (ensure) on first need. At most one non-deleted `bot_dm` MAY exist for the same `(tenant_id, bot_id, user_id)`.

#### Scenario: Ensure bot_dm on send

- **GIVEN** user U has user enablement for bot X in tenant A and no `bot_dm` yet
- **WHEN** a caller invokes `ImBotApi.send` targeting `(A, U)` with `botCode=X`
- **THEN** the system creates a `bot_dm` conversation with User and Bot members
- **AND** inserts `im_message` with `sender_type=bot`

#### Scenario: Reject send without enablement

- **GIVEN** user U lacks user enablement for bot X in tenant A
- **WHEN** `ImBotApi.send` targets `(A, U)` with `botCode=X`
- **THEN** the system rejects with a clear business error
- **AND** MUST NOT insert a message

### Requirement: ImBotApi cross-module business reach

The system MUST expose `ImBotApi` in `relayflow-module-im-api` as the sole write entry for cross-module business reach. Implementation MUST live in `im-biz`. Callers MUST NOT depend on `im-biz` or write `im_*` tables directly. `ImBotApi.send` MUST support target scope `SINGLE {tenantId, userId}` (required) and MAY support `{userId, fanout=ALL_ACTIVE_MEMBERSHIPS}`. Optional `dedupeKey` MUST be idempotent within `(tenant_id, bot_id, user_id, dedupe_key)` for the documented window.

#### Scenario: Single-tenant delivery

- **GIVEN** a valid bot and user enablement
- **WHEN** `system-biz` or `task-biz` calls `ImBotApi.send` with SINGLE target
- **THEN** the message is persisted in that tenant's `bot_dm` as `im_message`
- **AND** if the user is online, they receive `domain=im, type=message.new` via `RealtimeTransportApi`

#### Scenario: Optional membership fanout

- **GIVEN** user U has multiple ACTIVE memberships with enablement (or auto-fill) for the bot
- **WHEN** the caller explicitly sets `fanout=ALL_ACTIVE_MEMBERSHIPS`
- **THEN** the system delivers once per ACTIVE membership (separate `bot_dm` per tenant)
- **AND** MUST NOT fan out when fanout is not specified

#### Scenario: Dedupe idempotency

- **GIVEN** an existing unread or in-window message for the same `(tenant, bot, user, dedupeKey)`
- **WHEN** `send` repeats the same dedupeKey
- **THEN** the system MUST NOT create another unread business message (update-or-ignore; behavior MUST be stable and documented)

### Requirement: System sender semantics narrowed

Messages with `sender_type=system` MUST only represent in-conversation environment copy (e.g. member joined a group). They MUST NOT be used as a cross-module business notification bus. Business reach MUST use `sender_type=bot` via `ImBotApi` (or Bot Runtime outbound replies).

#### Scenario: Group join remains system

- **GIVEN** a successful group member invite inside im-biz
- **WHEN** the join tip is persisted
- **THEN** `sender_type=system`
- **AND** business modules MUST NOT be required to call `ImBotApi` for that tip

### Requirement: Group bot membership and mention (phased)

The system MUST allow bots as group conversation members. When a user @mentions a bot in a group, after message persistence the system MUST invoke Bot Ingress and dispatch via Bot Runtime by `handler_kind`. V1 MUST implement `platform` and `noop`; `webhook` MAY be a stub. Group bot delivery MAY be phased, but these requirements apply in this version. Outbound `ImBotApi` MUST NOT be blocked on unfinished group-bot steps.

#### Scenario: Attach bot to group

- **GIVEN** a group conversation and an enabled bot
- **WHEN** an authorized action adds the bot as a member
- **THEN** `im_conversation_member` records the bot member
- **AND** member/list APIs can distinguish bots from users

#### Scenario: Mention triggers ingress

- **GIVEN** a group with a bot member and a user message mentioning that bot
- **WHEN** the message is persisted
- **THEN** Bot Ingress/Runtime is invoked
- **AND** the system MUST NOT push a human-client WS envelope to the bot (bots have no login client)

#### Scenario: noop inbound

- **GIVEN** a bot with `handler_kind=noop`
- **WHEN** a user messages its `bot_dm` or @mentions it
- **THEN** Runtime MAY produce no reply
- **AND** Outbound `ImBotApi.send` for that bot MUST still work

### Requirement: Card content placeholder

The message content model MUST reserve a `card` shape (and future interactive actions) for Feishu-like interactive cards. Foundation MAY allow text plus deep-link metadata first. Full interactive callback auth/timeout/idempotency belongs to a later slice, but MUST NOT resurrect a parallel notify write model.

#### Scenario: Text reach allowed

- **GIVEN** `ImBotApi.send` with text and optional deep-link metadata
- **WHEN** the message is persisted
- **THEN** clients can render it in `bot_dm` and navigate the deep link

#### Scenario: Card type can persist as reserved

- **GIVEN** a sender uses the reserved `card` content type (field set per implementation contract)
- **WHEN** the message is persisted
- **THEN** `im_message` succeeds
- **AND** unimplemented interactive callbacks MUST NOT dual-write `infra_notify`

### Requirement: Conversation list includes bot_dm

The app conversation list MUST include the current user's `bot_dm` conversations in the current tenant, with unread and last preview, using the same list model as direct/group.

#### Scenario: List shows bot conversation

- **GIVEN** the user has a `bot_dm` with unread messages
- **WHEN** GET `/app-api/im/conversation/list`
- **THEN** the response includes an item with `type=bot_dm`
- **AND** includes unread count

## MODIFIED Requirements

### Requirement: Unified conversation model

The system MUST use a unified `im_conversation` model for direct, group, bot DM, and channel conversations, distinguished by `type`, without parallel per-type table structures.

#### Scenario: Direct conversation type

- **GIVEN** two users in a tenant
- **WHEN** the system creates or gets their conversation
- **THEN** `im_conversation.type` is `direct`
- **AND** only one direct conversation exists for that pair in the tenant

#### Scenario: Group conversation type

- **GIVEN** multiple users in a tenant
- **WHEN** the system creates a group chat
- **THEN** `im_conversation.type` is `group`
- **AND** membership is recorded in `im_conversation_member`

#### Scenario: Bot DM conversation type

- **GIVEN** one user and one enabled bot in a tenant
- **WHEN** the system ensures their conversation
- **THEN** `im_conversation.type` is `bot_dm`
- **AND** only one `bot_dm` exists for `(tenant, bot, user)`

#### Scenario: Channel conversation type

- **GIVEN** a channel and its subscribers
- **WHEN** the system creates a channel conversation
- **THEN** `im_conversation.type` is `channel`
- **AND** channel product behavior MAY remain deferred while the type enum is retained

### Requirement: Cross-module IM API

The system MUST expose cross-module contracts in `relayflow-module-im-api`; other modules MUST NOT depend on `im-biz` implementation types. Business reach MUST use `ImBotApi`. Environment copy MAY use `ImMessageApi.sendSystemMessage` (or equivalent) only for environment semantics.

#### Scenario: Get or create direct

- **GIVEN** module A depends on `relayflow-module-im-api`
- **WHEN** it calls `ImConversationApi.getOrCreateDirectConversation(userA, userB)`
- **THEN** a conversation id is returned
- **AND** it MUST NOT access `im_*` mappers directly

#### Scenario: System environment message send

- **GIVEN** a module needs environment copy in an existing conversation (not business reach)
- **WHEN** it calls `ImMessageApi.sendSystemMessage(conversationId, content, publisher)`
- **THEN** the message is persisted with `sender_type=system`
- **AND** online members receive `domain=im, type=message.new`

#### Scenario: Business reach via ImBotApi

- **GIVEN** a business event such as task due or member invite
- **WHEN** delivering a user-visible reminder across modules
- **THEN** the caller MUST invoke `ImBotApi.send`
- **AND** MUST NOT call removed `NotifyInboxApi`

## REMOVED Requirements

### Requirement: Notify inbox API placeholder

**Reason**: Business reach moves to IM Bot (`ImBotApi` + `im_message`); the parallel "must not write notifications into `im_message`" channel is retired.  
**Migration**: Callers switch to `ImBotApi.send`; delete `NotifyInboxApi` and `infra_notify`.
