# 即时通讯模块规格（im）

## Purpose

定义单聊、群聊、频道、消息持久化、附件消息、已读回执与在线状态相关工作台与 API 行为。

## Requirements

### 需求：单聊

系统应支持两名用户之间的一对一会话。

#### 场景：发送单聊消息

- 给定 用户 A 与用户 B 均为活跃状态
- 当 用户 A 向用户 B 发送消息
- 那么 消息持久化至 `im_` 前缀表
- 并且 若用户 B 在线则经 WebSocket 收到消息

### 需求：群聊

系统应支持多人群聊及成员管理。

#### 场景：群消息投递

- 给定 包含成员 A、B、C 的群组
- 当 成员 A 发送群消息
- 那么 各成员按在线状态收到消息

### 需求：频道广播

系统应支持只读频道，向订阅用户广播消息。

#### 场景：频道发帖

- 给定 一个频道及其订阅用户
- 当 授权用户向频道发帖
- 那么 订阅者收到广播消息

### 需求：先持久化再确认

系统必须先将消息写入数据库，再向发送方客户端返回发送确认。

#### 场景：离线安全发送

- 给定 数据库可用
- 当 客户端发送一条消息
- 那么 在返回发送 ACK 之前，PostgreSQL 中已存在该消息记录

### 需求：IM 表前缀

IM 模块全部数据表必须使用 `im_` 前缀。

#### 场景：表命名

- 给定 新建一张 IM 表
- 当 应用迁移脚本
- 那么 表名以 `im_` 开头

### 需求：IM API 路径

IM 的 REST 端点：管理操作为 `/admin-api/im/`，用户操作为 `/app-api/im/`。

#### 场景：用户端会话列表

- 给定 已认证的用户端用户
- 当 请求 `/app-api/im/conversation/list`
- 那么 系统返回该用户的会话列表

### 需求：IM 模块拆分

IM 模块必须拆分为 `relayflow-module-im-api` 与 `relayflow-module-im-biz`。

#### 场景：跨模块使用

- 给定 其他模块需要 IM 类型或 API
- 当 添加 Maven 依赖
- 那么 仅使用 `relayflow-module-im-api`

### 需求：统一会话模型

系统必须使用统一的 `im_conversation` 模型表示单聊、群聊与频道；通过 `type` 区分，不得为每种类型单独建平行表结构。

#### 场景：单聊会话类型

- 给定 租户内两名用户
- 当 系统创建或获取两人会话
- 那么 `im_conversation.type` 为 `direct`
- 并且 同一对用户在同一租户内仅存在一个 direct 会话

#### 场景：群聊会话类型

- 给定 多名租户内用户
- 当 系统创建群聊
- 那么 `im_conversation.type` 为 `group`
- 并且 成员关系记录在 `im_conversation_member`

#### 场景：频道会话类型

- 给定 一个频道及其订阅用户
- 当 系统创建频道
- 那么 `im_conversation.type` 为 `channel`
- 并且 订阅者 `role` 为 `subscriber`

#### 场景：Bot DM 会话类型

- 给定 租户内一名用户与一名已启用 Bot
- 当 系统 ensure 其会话
- 那么 `im_conversation.type` 为 `bot_dm`
- 并且 同一 `(tenant, bot, user)` 仅存在一个 `bot_dm`

### 需求：会话成员与已读水位

系统必须为每个会话成员维护 `read_seq` 与 `unread_count`，用于已读状态与未读数展示。

#### 场景：上报已读

- 给定 用户是会话成员且持有有效 JWT
- 当 客户端经 WebSocket 或 REST 上报已读至 `seq=N`
- 那么 系统更新该成员 `read_seq` 为 `max(当前值, N)`
- 并且 重新计算 `unread_count`

#### 场景：非成员不可读

- 给定 用户不是会话成员
- 当 请求该会话消息列表或上报已读
- 那么 系统拒绝并返回明确业务错误

### 需求：会话内消息序号

系统必须为每条会话消息分配在该会话内单调递增的 `seq`；客户端同步与已读均以 `seq` 为准。

#### 场景：发送分配 seq

- 给定 用户向会话发送一条新消息
- 当 消息持久化成功
- 那么 该消息 `seq` 大于该会话已有消息的最大 `seq`

#### 场景：增量拉取

- 给定 用户是会话成员
- 当 请求 `/app-api/im/message/list?conversationId=&afterSeq=N`
- 那么 系统返回该会话中 `seq > N` 的消息，按 `seq` 升序

### 需求：客户端发送幂等

系统必须支持 `client_msg_id`（客户端 UUID）；同一租户内重复提交相同 `client_msg_id` 时，必须返回已有消息而非重复插入。

#### 场景：重复 client_msg_id

- 给定 用户已用 `client_msg_id=X` 成功发送消息
- 当 再次提交相同 `client_msg_id=X`
- 那么 系统返回与原消息相同的 server id 与 seq
- 并且 不得产生第二条消息记录

### 需求：消息内容块结构

系统必须使用结构化 `content_json`（Content Block 数组）存储消息内容；V1 须支持 `text` 与 `file` 块类型，并预留 `link`、`deeplink`、`card`、`actions`、`mention` 扩展。地基期 Bot 触达允许 `text` + `deeplink` 元数据；可交互 card callback 属后续切片。

#### 场景：纯文本消息

- 给定 用户发送文本内容
- 当 消息持久化
- 那么 `content_json.blocks` 含至少一个 `type=text` 块

#### 场景：文件消息引用 infra

- 给定 用户发送附件且 `fileId` 属于当前租户
- 当 消息持久化
- 那么 `content_json.blocks` 含 `type=file` 且引用 `infra_file.fileId`
- 并且 消息 API 不得代理 multipart 文件字节

### 需求：消息发布者类型

系统必须记录消息发布者类型 `sender_type`（`user` | `system` | `bot` | `app`）；V1 须实现 `user`、`system` 与 `bot`（业务触达经 `ImBotApi`）。

#### 场景：用户发送

- 给定 普通用户发送消息
- 当 消息持久化
- 那么 `sender_type=user` 且 `sender_id` 为当前用户 id

#### 场景：系统消息

- 给定 系统事件（如成员加入群）
- 当 通过 `ImMessageApi` 发送系统消息
- 那么 `sender_type=system`
- 并且 不得伪造为普通用户发送

### 需求：跨模块 IM API

系统必须在 `relayflow-module-im-api` 暴露跨域契约；其他模块不得依赖 `im-biz` 实现类。

#### 场景：获取或创建单聊

- 给定 模块 A 依赖 `relayflow-module-im-api`
- 当 调用 `ImConversationApi.getOrCreateDirectConversation(userA, userB)`
- 那么 返回会话 id
- 并且 不得直接访问 `im_*` Mapper

#### 场景：系统环境消息

- 给定 模块需向已有会话投递环境文案（非跨模块业务触达）
- 当 调用 `ImMessageApi.sendSystemMessage(conversationId, content, publisher)`
- 那么 消息持久化至 `im_message` 且 `sender_type=system`
- 并且 在线成员经 WebSocket 收到 `domain=im, type=message.new`

#### 场景：跨模块业务触达

- 给定 业务事件（邀请、任务到期、审批待办等）
- 当 跨模块向用户投递可见提醒
- 那么 调用方 MUST 使用 `ImBotApi.send`
- 并且 MUST NOT 调用已删除的 `NotifyInboxApi`

### 需求：域事件发布入口

系统必须在平台层提供 `RealtimeEventPublisher` 入口；`domain=notify` 不再作为业务触达推送通道，实时统一走 `domain=im`；`domain=presence` 可为占位实现。

#### 场景：非 IM 模块发布事件

- 给定 `bpm-biz` 依赖 `infra-api` 中的 `RealtimeEventPublisher`
- 当 发布 `domain=notify` 事件
- 那么 调用不得抛出「接口不存在」类错误
- 并且 V1 允许 no-op 实现（不得再作为业务触达写真源）

#### 场景：IM 不得绕过传输层

- 给定 IM 业务需向用户推送 envelope
- 当 消息已持久化
- 那么 须通过 `RealtimeTransportApi` 投递
- 并且 不得在 `im-biz` 内直接持有 WebSocket Session 引用

### Requirement: Bot catalog and enablement

The system MUST maintain a platform-level bot catalog (not owned by a tenant) and record enablement at tenant and user layers. Bots MUST NOT be persisted as `sys_user` rows. Seeded platform assistants (`org-assistant`, `task-bot`, `approval-bot`, `calendar-bot`, etc.) MUST be stored as `type=system`.

#### Scenario: Bot is not a login user

- **GIVEN** platform seed inserts a bot definition
- **WHEN** querying `sys_user`
- **THEN** no login account row exists for that bot

#### Scenario: System bot catalog for organization reach

- **GIVEN** seeded system bots
- **WHEN** a caller sends an organization invite reminder
- **THEN** the caller uses `botCode=org-assistant`
- **AND** MUST NOT use retired `invite-helper`

### Requirement: calendar-bot platform seed

The system MUST seed a platform bot with code `calendar-bot` as `type=system` (calendar assistant) alongside existing assistants such as `org-assistant` and `task-bot`. System-type bots MUST remain reachable via `ImBotApi.send` without tenant/user enablement rows.

#### Scenario: Seed present

- **WHEN** the platform bot catalog is migrated for calendar V1
- **THEN** a bot row with `code=calendar-bot` and `type=system` exists

#### Scenario: Send without subscription

- **WHEN** `calendar-biz` calls `ImBotApi.send` with `botCode=calendar-bot` and a valid SINGLE target
- **THEN** delivery proceeds without requiring `im_bot_tenant_enablement` or `im_bot_user_enablement`

### Requirement: docs-bot platform seed

The system MUST seed a platform bot with code `docs-bot` as `type=system` (document assistant) alongside existing assistants such as `org-assistant`, `task-bot`, and `calendar-bot`. System-type bots MUST remain reachable via `ImBotApi.send` without requiring tenant/user enablement rows for delivery authorization.

#### Scenario: Seed present

- **WHEN** the platform bot catalog is migrated for docs share V1
- **THEN** a bot row with `code=docs-bot` and `type=system` exists

#### Scenario: Send without subscription

- **WHEN** `docs-biz` calls `ImBotApi.send` with `botCode=docs-bot` and a valid SINGLE target
- **THEN** delivery proceeds without requiring `im_bot_tenant_enablement` or `im_bot_user_enablement` for authorization

### Requirement: Bot reachability for ImBotApi.send

The system MUST classify bots with catalog field `type`. When `type` is `system`, `ImBotApi.send` MUST deliver without requiring `im_bot_tenant_enablement` or `im_bot_user_enablement`. When `type` is not `system`, delivery MUST be allowed if **either** tenant or user subscription exists (union). The system MUST NOT require both simultaneously.

#### Scenario: System bot delivers without subscriptions

- **GIVEN** a seeded bot with `type=system` (e.g. `org-assistant`)
- **AND** no enablement rows for target `(tenant, user, bot)`
- **WHEN** `ImBotApi.send` targets a valid ACTIVE membership
- **THEN** the message is persisted in `bot_dm` and realtime push is attempted

#### Scenario: Non-system bot denied when neither subscription exists

- **GIVEN** a non-system bot with neither tenant nor user subscription
- **WHEN** `ImBotApi.send` is invoked
- **THEN** the send fails with a bot-not-enabled style error

### Requirement: bot_dm conversation

The system MUST support `im_conversation.type=bot_dm` for one-to-one User↔Bot conversations. Conversations MUST be lazy-created on first need.

#### Scenario: Ensure bot_dm on send

- **GIVEN** user U is reachable for bot X in tenant A
- **WHEN** `ImBotApi.send` targets `(A, U)` with `botCode=X`
- **THEN** the system creates or reuses a `bot_dm` conversation
- **AND** inserts `im_message` with `sender_type=bot`

### Requirement: ImBotApi cross-module business reach

The system MUST expose `ImBotApi` in `relayflow-module-im-api` as the sole write entry for cross-module business reach. `ImBotApi.send` MUST support `SINGLE {tenantId, userId}` and MAY support `{userId, fanout=ALL_ACTIVE_MEMBERSHIPS}`. Optional `dedupeKey` MUST be idempotent within `(tenant_id, bot_id, user_id, dedupe_key)`.

#### Scenario: Single-tenant delivery

- **GIVEN** a valid bot and reachable user
- **WHEN** `system-biz` or `task-biz` calls `ImBotApi.send` with SINGLE target
- **THEN** the message is persisted in that tenant's `bot_dm`
- **AND** online users receive `domain=im, type=message.new`

#### Scenario: Dedupe idempotency

- **GIVEN** an existing in-window message for the same `(tenant, bot, user, dedupeKey)`
- **WHEN** `send` repeats the same dedupeKey
- **THEN** the system MUST NOT create another unread business message

### Requirement: System sender semantics narrowed

Messages with `sender_type=system` MUST only represent in-conversation environment copy (e.g. member joined a group). They MUST NOT be used as a cross-module business notification bus. Business reach MUST use `sender_type=bot` via `ImBotApi` (or Bot Runtime outbound replies).

#### Scenario: Group join remains system

- **GIVEN** a successful group member invite inside im-biz
- **WHEN** the join tip is persisted
- **THEN** `sender_type=system`
- **AND** business modules MUST NOT be required to call `ImBotApi` for that tip

### Requirement: Group bot membership and mention (phased)

The system MUST allow bots as group conversation members. When a user @mentions a bot in a group, after message persistence the system MUST invoke Bot Ingress and dispatch via Bot Runtime by `handler_kind`. V1 MUST implement `platform` and `noop`; `webhook` MAY be a stub. Group bot delivery MAY be phased across `im-bot-group-member` / `im-bot-group-mention` / `im-bot-runtime-platform`, but these requirements apply in this version. Outbound `ImBotApi` MUST NOT be blocked on unfinished group-bot steps.

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

### Requirement: Bot Runtime SPI and handler kinds

The IM module MUST provide a Bot Runtime that dispatches inbound bot events by catalog `handler_kind`. V1 MUST implement `noop` (consume without reply) and `platform` (dispatch to registered in-process handlers; missing handler behaves as noop). `webhook` MAY be stubbed and MUST NOT perform external HTTP in V1. Runtime replies MUST be persisted in the same conversation with `sender_type=bot` and realtime-pushed only to User members. Card action handling MUST use a separate ingress/SPI and MUST NOT be implemented as Bot Runtime handlers. Business modules MUST NOT own group @-receive logic; they reach users via `ImBotApi.send` or domain events.

#### Scenario: noop produces no reply

- **GIVEN** a bot with `handler_kind=noop`
- **WHEN** Bot Runtime receives an inbound mention or bot_dm message
- **THEN** Runtime produces no reply message
- **AND** Outbound `ImBotApi.send` for that bot still works

#### Scenario: platform dispatches in-process handler

- **GIVEN** a bot with `handler_kind=platform` and a registered platform handler
- **WHEN** Runtime receives inbound context for that bot
- **THEN** the registered handler is invoked
- **AND** any reply is stored as `sender_type=bot` in the same conversation

#### Scenario: webhook is stub only

- **GIVEN** a bot with `handler_kind=webhook`
- **WHEN** Runtime receives inbound context
- **THEN** the system does not perform an external HTTP callback in V1
- **AND** behavior is documented as unimplemented/stub

#### Scenario: Bot members do not receive client push

- **GIVEN** a Runtime reply is persisted in a group
- **WHEN** realtime fanout runs
- **THEN** only User members may receive `domain=im` client envelopes
- **AND** Bot subjects MUST NOT be treated as login clients

### Requirement: Group bot mention ingress

After a user message is persisted in a group that has bot members, if the message content mentions one or more of those bots (structured `mention` blocks are the source of truth), the system MUST invoke Bot Ingress for each mentioned bot that is an active group member. Ingress MUST hand off to Bot Runtime dispatch. The system MUST NOT push a human-client realtime envelope targeted at the bot subject. Persistence of the user message MUST NOT be rolled back if Ingress/Runtime fails (best-effort inbound).

#### Scenario: Mention triggers ingress

- **GIVEN** a group with bot X as a member
- **AND** a user message whose content mentions bot X
- **WHEN** the message is persisted successfully
- **THEN** Bot Ingress is invoked for bot X
- **AND** Bot Runtime dispatch is attempted
- **AND** the system MUST NOT push a human-client WS envelope to the bot

#### Scenario: Mention of non-member bot is ignored

- **GIVEN** a message mentions bot Y that is not an active member of the group
- **WHEN** the message is persisted
- **THEN** Ingress MUST NOT dispatch Runtime for bot Y as a group mention
- **AND** the user message remains visible to human members

#### Scenario: Ingress failure does not drop user message

- **GIVEN** Bot Runtime throws or times out after a valid mention
- **WHEN** inbound handling fails
- **THEN** the original user message remains persisted
- **AND** the failure is logged server-side

### Requirement: Card content placeholder

The message content model MUST support a `card` shape for Feishu-like interactive cards. Text plus deep-link metadata remains allowed for lightweight reach. Interactive callback auth/timeout/idempotency is implemented via the interactive card action SPI and MUST NOT resurrect a parallel notify write model.

#### Scenario: Text reach allowed

- **GIVEN** `ImBotApi.send` with text and optional deep-link metadata
- **WHEN** the message is persisted
- **THEN** clients can render it in `bot_dm` and navigate the deep link

#### Scenario: Card type persists

- **GIVEN** a sender uses the `card` content type per implementation contract
- **WHEN** the message is persisted
- **THEN** `im_message` succeeds
- **AND** card interactions use Card Action Ingress rather than `infra_notify`

### Requirement: Interactive card send and action SPI

The system MUST support Feishu-like interactive cards as IM message content. `ImBotApi.send` MUST accept a card document (including `generic.v1` template with header/fields/actions). Clients MUST render cards in conversation history. Action behaviors are limited to `open_url` (client-local navigation) and `callback` (server SPI, optional form values). The system MUST expose `POST /app-api/im/card/action` that authenticates the caller as a User member of the conversation, enforces `meta.expiresAt`, supports idempotency via `clientActionId` (or equivalent), and dispatches to an in-process `CardActionHandler` resolved by `actionKey`. Handlers are defined in `im-api` and implemented by business modules (or a documented demo handler). Successful callbacks MAY return toast and/or an updated card snapshot that IM patches onto the message and pushes via realtime. System bots MUST NOT require external callback URLs. The system MUST NOT dual-write `infra_notify` or use `domain=notify` as a business write model for card interactions. Bot Runtime dialogue inbound MUST remain a separate path from card actions.

#### Scenario: Send card via ImBotApi

- **GIVEN** a reachable user and bot
- **WHEN** a caller invokes `ImBotApi.send` with a card document
- **THEN** the message is persisted with card content
- **AND** online User members receive `domain=im, type=message.new`

#### Scenario: open_url does not hit action API

- **GIVEN** a card action with `behavior.type=open_url`
- **WHEN** the user activates that action
- **THEN** the client navigates using the route/URL locally
- **AND** MUST NOT call `POST /app-api/im/card/action` for that activation

#### Scenario: callback dispatches handler

- **GIVEN** a card action with `behavior.type=callback` and a registered `CardActionHandler` for its `actionKey`
- **WHEN** the user submits `POST /app-api/im/card/action` with valid membership and unexpired card
- **THEN** the handler is invoked with opaque `payload` and optional `formValues`
- **AND** the HTTP response may include toast and updated message/card data

#### Scenario: Expired card rejects interaction

- **GIVEN** `meta.expiresAt` is in the past
- **WHEN** the user calls the card action API
- **THEN** the system rejects the interaction
- **AND** MUST NOT invoke the business handler

#### Scenario: Idempotent clientActionId

- **GIVEN** a successful callback for a `clientActionId`
- **WHEN** the same caller retries with the same `clientActionId`
- **THEN** the system does not apply the business side effect twice
- **AND** returns a stable success-equivalent response

#### Scenario: No notify dual-write

- **WHEN** a card is sent or a callback completes
- **THEN** the system MUST NOT insert into `infra_notify`
- **AND** MUST NOT require `domain=notify` as the write model

### Requirement: Conversation list includes bot_dm

The app conversation list MUST include the current user's `bot_dm` conversations with unread and last preview.

#### Scenario: List shows bot conversation

- **GIVEN** the user has a `bot_dm` with unread messages
- **WHEN** GET `/app-api/im/conversation/list`
- **THEN** the response includes an item with `type=bot_dm`
- **AND** includes unread count

### Requirement: Group bot membership management

The system MUST allow authorized group members to add and remove bots as conversation members with `subject_type=bot`. Member list APIs MUST distinguish bots from users and expose bot display metadata (name/avatar from `im_bot`). Adding an already-present bot MUST be idempotent. Outbound `ImBotApi` MUST NOT depend on group bot membership. The system MUST NOT persist bots as `sys_user` rows. The group-embeddable catalog used for attach MUST follow the owner-scoped embeddable bot catalog rules (system delivery bots excluded; agent carriers only for their `owner_user_id`).

#### Scenario: Attach bot to group

- **GIVEN** a `group` conversation and an embeddable bot the caller is allowed to attach
- **WHEN** an authorized caller adds the bot via the group bot membership API
- **THEN** `im_conversation_member` records the bot with `subject_type=bot`
- **AND** member/list responses can distinguish the bot from users

#### Scenario: Remove bot from group

- **GIVEN** a group that already has bot X as a member
- **WHEN** an authorized caller removes bot X
- **THEN** the bot membership is removed or soft-deleted per existing member semantics
- **AND** subsequent member lists do not include bot X as an active member

#### Scenario: Idempotent attach

- **GIVEN** bot X is already a member of the group
- **WHEN** attach is requested again for bot X
- **THEN** the system does not create a duplicate active membership
- **AND** the API succeeds without error (no-op or explicit already-member result)

#### Scenario: System join tip on attach

- **GIVEN** a successful bot attach
- **WHEN** the membership is persisted
- **THEN** the system MAY persist a `sender_type=system` environment message in the group
- **AND** MUST NOT use `ImBotApi` for that tip

### 需求：群聊 REST API

系统 MUST 提供用户端群聊 REST：建群、邀请成员、查询群成员、管理 Bot 成员；群消息 MUST 复用现有 `conversationId` 消息发送/列表接口。

#### 场景：创建群聊

- 给定 已认证的有效组织成员
- 当 POST `/app-api/im/group/create` 提交有效群名与至少一名成员
- 那么 系统创建 `im_conversation(type=group)`、`im_group` 及成员关系（创建者为 owner）
- 并且 返回 `conversationId` 与 `groupId`

#### 场景：邀请成员

- 给定 用户为群成员
- 当 POST `/app-api/im/group/members/add` 提交新成员 userId 列表
- 那么 新成员写入 `im_conversation_member`
- 并且 为每次加入事件持久化一条系统消息

#### 场景：成员列表含 Bot

- 给定 群内同时有 User 与 Bot 成员
- 当 GET 群成员列表
- 那么 响应区分 `subjectType=user|bot`
- 并且 Bot 项包含可展示的名称（来自 Bot 目录）

#### 场景：会话列表含群聊

- 给定 用户持有有效 JWT
- 当 请求 `/app-api/im/conversation/list`
- 那么 返回 direct 与 group 会话
- 并且 group 项包含 `memberCount`

#### 场景：群消息展示发送者昵称

- 给定 群会话中存在其他成员发送的消息
- 当 客户端拉取消息列表或收到 `message.new`
- 那么 每条用户消息可解析出发送者展示名

### 需求：文件与图片消息

系统 MUST 接受 `type=image|file` 消息，其 `content_json` 含至少一个引用当前租户有效 `infra_file.fileId` 的 `file` 块；列表响应 MUST 为 file 块填充可下载 URL。

#### 场景：发送图片消息

- 给定 成员发送 `type=image` 且 file 块 mimeType 以 `image/` 开头
- 当 消息持久化
- 那么 会话 `lastMsgPreview` 为 `[图片]`

#### 场景：拒绝无效 fileId

- 给定 fileId 不存在或不属于当前租户
- 当 用户尝试发送附件消息
- 那么 API 返回明确业务错误
- 并且 不得插入消息记录

#### 场景：列表填充 downloadUrl

- 给定 消息含 file 块
- 当 成员请求消息列表
- 那么 各 file 块包含指向 `/app-api/infra/file/download` 的 `downloadUrl`

### 需求：已读水位查询与推送

系统 MUST 向会话成员暴露各成员 `readSeq`；成员成功上报已读后 MUST 向其他在线成员 fanout `domain=im, type=read.updated`。

#### 场景：查询已读水位

- 给定 用户为会话成员
- 当 GET `/app-api/im/conversation/read-status?conversationId=`
- 那么 返回各成员 `userId` 与 `readSeq`

#### 场景：非成员不可查

- 给定 用户非会话成员
- 当 请求 read-status
- 那么 系统拒绝并返回业务错误

#### 场景：已读变更推送

- 给定 成员成功调用 `/app-api/im/conversation/read` 且 readSeq 提升
- 当 fanout 完成
- 那么 其他会话成员收到 `read.updated` 事件（含 userId、readSeq）

### 需求：批量在线状态

系统 MUST 允许已认证成员批量查询当前租户内用户的在线状态（基于 WebSocket 会话）。

#### 场景：批量查询

- 给定 成员请求 GET `/app-api/im/presence/batch?userIds=`
- 当 userIds 属于当前租户 ACTIVE 成员
- 那么 返回各 userId 的 `online` 布尔值

#### 场景：忽略非本租户 ID

- 给定 请求含他租户 userId
- 当 处理 batch 查询
- 那么 忽略这些 ID 且不报错

### Requirement: Conversation search API for workspace

The system SHALL expose `GET /app-api/im/conversation/search` for authenticated users to search their conversations by title or direct-chat peer display name.

#### Scenario: Conversation search endpoint

- **WHEN** an authenticated user requests `GET /app-api/im/conversation/search?keyword=项目&limit=5`
- **THEN** the system returns up to 5 conversations where the user is a member and the keyword matches
- **AND** results are scoped to the JWT tenant

---

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

### Requirement: Agent carrier bot provisioning API

The IM module SHALL expose `ImBotProvisioningApi` so the Agent domain can create, update, and retire tenant carrier bots bound to an Agent (`type=tenant`, `handler_kind=platform`, `owner_agent_id`). Creating an Agent MUST provision (or ensure) its carrier bot and store `carrier_bot_id`. Deleting an Agent MUST retire its carrier bot. Provisioning SHALL be idempotent per `agentId`.

#### Scenario: Provision bot when agent needs carrier

- **WHEN** the Agent domain requests provisioning for an Agent carrier
- **THEN** IM creates or ensures a non-system bot bound to that Agent
- **AND** the Agent stores the bot identifier as its carrier reference

#### Scenario: Retire bot when agent is deleted

- **WHEN** an Agent is deleted
- **THEN** IM retires the bound carrier bot (disabled / not dispatchable)

### Requirement: Carrier inbound to AgentRuntime

Inbound user messages to a carrier bot (group @mention of a member bot, or bot_dm) SHALL be dispatched to the Agent runtime under the triggering user's security context. The assistant reply SHALL be written as an IM bot message in the same conversation. Those turns MUST NOT be written to `agent_session`.

#### Scenario: bot_dm inbound replies in IM

- **WHEN** a user sends a text message in a bot_dm with a carrier bot
- **THEN** the Agent runtime may produce a reply
- **AND** the reply is persisted via bot reply in that conversation
- **AND** no `agent_session_message` row is created for that turn

### Requirement: Managed groups for member picker web

系统 MUST 能确定当前用户可管理的群组（至少含群主；管理员可选）。`-web` MAY 通过会话列表与成员接口前端聚合；后续 MAY 提供专用 `GET /app-api/im/group/managed`。

#### Scenario: Owner group appears in managed list

- **WHEN** 用户是某群的 owner 并打开选择器「我管理的群组」
- **THEN** 该群出现在列表中且可展开/勾选其用户成员

### Requirement: Group-embeddable bot catalog by owner

群「添加机器人」目录 MUST 仅包含当前登录用户可作为主人嵌入群聊的 bot。目录 MUST NOT 包含仅用于系统投递、不具备入群服务能力的 platform system bot（含 `handler_kind=noop` 的 system 助手）。对绑定 Agent 的 carrier bot（`owner_agent_id` 非空），目录 MUST 仅在 `owner_user_id` 等于当前用户时返回该项。`im_bot` MUST 持久化 carrier 的 `owner_user_id`，并在 provision 时写入。

#### Scenario: System delivery bots excluded

- **GIVEN** 租户存在已启用的 `type=system` 投递 bot（如 `task-bot`）
- **WHEN** 群成员请求 `GET /app-api/im/group/bots/catalog`
- **THEN** 响应不包含这些 system 投递 bot

#### Scenario: Owner sees own agent carrier

- **GIVEN** 用户 A 拥有已启用的 agent，且已 provision 对应 carrier bot（`owner_user_id=A`）
- **WHEN** 用户 A 请求该群的 bot catalog
- **THEN** 响应包含该 carrier bot（含名称与 code）
- **AND** `alreadyMember` 正确反映是否已在群中

#### Scenario: Non-owner does not see others agent carrier

- **GIVEN** 用户 B 的 agent carrier 已启用
- **WHEN** 用户 A（非主人）请求 bot catalog
- **THEN** 响应不包含用户 B 的 carrier bot

### Requirement: Add agent carrier requires owner

将绑定 Agent 的 carrier bot 加入群聊时，调用者 MUST 为该 bot 的 `owner_user_id`。群主身份 MUST 仍满足现有群 Bot 成员管理授权；非主人 MUST NOT 成功添加他人的 carrier bot。

#### Scenario: Owner group-owner adds carrier

- **GIVEN** 用户 A 为群主且为某 carrier 的 `owner_user_id`
- **WHEN** A 请求将该 bot 加入群
- **THEN** 成员关系写入成功（或幂等已存在）

#### Scenario: Non-owner cannot add carrier even if group owner

- **GIVEN** 用户 A 为群主但不是某 carrier 的主人
- **WHEN** A 请求添加该 bot
- **THEN** 系统拒绝添加

### Requirement: Tenant IM product settings persistence

系统 MUST 按租户持久化即时消息组织策略，至少覆盖：对话群默认人数上限、用户规则集、群组规则集、公开群策略、部门群退出策略、群成员导出策略、消息撤回与编辑开关及时间窗。无行时 MUST 使用代码默认（默认群上限 **50**；撤回/编辑默认允许且时间窗 **24 小时**；公开群默认允许；部门群退出默认允许；导出默认禁止任何人）。

#### Scenario: Default when no row

- **WHEN** 租户从未保存过 IM 产品设置
- **THEN** 生效策略使用上述代码默认
- **AND** 读路径 MUST NOT 强制要求预插行（若实现选择 A 类 ensure，亦须与默认语义一致）

#### Scenario: Admin save round-trip

- **WHEN** 管理员经 Admin API 保存有效策略
- **THEN** 再次读取返回已保存值
- **AND** 工作台强制执行使用新值

### Requirement: Group size control for chat groups only

群规模管控 MUST 仅作用于对话群（`type=group` 且非被排除的群种类）。企业默认上限可选 **5 / 10 / 30 / 50** 或自定义正整数，默认 **50**。补充规则 MUST 支持：

1. **用户规则集**：选择部门与/或用户，规定其可发起群聊的人数上限；可多条。
2. **群组规则集**：选择本企业已创建群，规定该群人数上限；可多条。

上限解析 MUST 为：群组规则 > 发起人用户/部门规则 > 企业默认。部门群等被排除种类 MUST NOT 受默认/用户规则上限约束（与飞书说明对齐的 V1 子集）。

#### Scenario: Create group respects initiator limit

- **WHEN** 成员创建对话群且初始成员数（含自己）超过其生效上限
- **THEN** 系统拒绝创建并返回明确业务错误

#### Scenario: Add members respects group limit

- **WHEN** 向已有对话群添加成员将使总人数超过该群生效上限
- **THEN** 系统拒绝添加
- **AND** MUST NOT 因下调上限而自动移除已有成员

#### Scenario: User rule for department

- **WHEN** 管理员为某部门配置用户规则上限为 100，且企业默认为 50
- **THEN** 该部门成员发起建群时生效上限为 100（在未被更高优先级规则覆盖时）

#### Scenario: Group rule overrides default

- **WHEN** 某群存在群组规则上限 30，企业默认为 50
- **THEN** 该群加成员以 30 为上限

#### Scenario: Only current-tenant groups in group rules

- **WHEN** 管理员配置群组规则集
- **THEN** 可选群 MUST 仅限当前租户的对话群
- **AND** 他租户会话 MUST NOT 可选中

### Requirement: Group permission control

系统 MUST 提供与飞书「群权限管控」语义对齐的三项组织策略：

1. **进群管理**：允许或禁止将群设为「公开」（可被企业内搜索并加入）；允许时群主/管理员可在群设置开启可搜索。
2. **部门群管理**：允许或禁止部门成员退出部门群。
3. **群成员信息导出**：禁止任何人导出；或允许群主与群管理员导出；或仅允许指定成员在担任群主/管理员时导出。

#### Scenario: Deny public groups

- **WHEN** 组织策略禁止设置公开群
- **THEN** 工作台 MUST NOT 允许将群标为可搜索公开
- **AND** 公开群搜索加入不可用（或对已有公开群按实现约定关闭）

#### Scenario: Dept group leave denied

- **WHEN** 策略禁止部门成员退出部门群，且用户为该部门群普通成员
- **WHEN** 用户请求退出
- **THEN** 系统拒绝退出

#### Scenario: Member export denied

- **WHEN** 导出策略为禁止任何人导出
- **THEN** 群主与管理员亦不可导出群成员信息

### Requirement: Public searchable groups

在组织允许公开群时，系统 MUST 支持对话群的「可被搜索」标记；企业成员 MUST 能搜索公开群并加入（V1：直接加入，无需审批流）。

#### Scenario: Search and join public group

- **GIVEN** 组织允许公开群且群主已开启可搜索
- **WHEN** 同租户非成员用户搜索并加入该群
- **THEN** 用户成为群成员
- **AND** 人数仍受该群生效上限约束

### Requirement: Department group minimal model

系统 MUST 支持最小部门群模型（群种类可区分普通对话群与部门群，并可关联部门）。部门群退出行为 MUST 受组织「部门群管理」策略约束。完整的部门成员自动同步可分期，但本能力落地前「部门群退出」策略不得仅有无执行的死开关。

#### Scenario: Identify department group

- **WHEN** 会话为部门群
- **THEN** 客户端与 API 可区分其与普通对话群
- **AND** 退出路径应用部门群策略而非普通群自由退出

### Requirement: Group member export

在导出策略允许时，系统 MUST 允许授权主体导出群成员信息（字段对齐名片可见信息范围）。导出 MUST 限于当前租户该群成员。

#### Scenario: Owner exports when allowed

- **GIVEN** 策略允许群主与管理员导出
- **WHEN** 群主请求导出
- **THEN** 系统返回可下载的成员信息文件或等价载荷

### Requirement: Message recall org policy

系统 MUST 支持组织级「是否允许企业成员撤回已发送消息」及时间窗。该设置 MUST NOT 剥夺群主/群管理员对群内消息的撤回能力（与飞书说明一致）。撤回后消息对会话成员 MUST 展示为已撤回占位，不得继续展示原文。

#### Scenario: Member recall within window

- **GIVEN** 组织允许撤回且时间窗为消息发出后 24 小时内
- **WHEN** 发送者在时限内撤回本人消息
- **THEN** 消息对成员显示为已撤回
- **AND** 经 WebSocket 或列表刷新对其他成员可见

#### Scenario: Member recall disabled

- **GIVEN** 组织不允许撤回
- **WHEN** 普通成员尝试撤回本人消息
- **THEN** 系统拒绝
- **AND** 群主或群管理员仍可按产品规则撤回群内消息

#### Scenario: Recall after window

- **GIVEN** 允许撤回但已超过时间窗
- **WHEN** 普通成员尝试撤回
- **THEN** 系统拒绝

### Requirement: Message edit org policy

系统 MUST 支持组织级「是否允许企业成员编辑已发送消息」及时间窗。编辑 MUST 仅限发送者本人（V1）；超时或禁用时 MUST 拒绝。编辑后 MUST 保留同一 `seq`，并标记已编辑。

#### Scenario: Edit within window

- **GIVEN** 组织允许编辑且在时间窗内
- **WHEN** 发送者编辑本人文本消息
- **THEN** 内容更新且对其他成员同步
- **AND** 客户端可展示已编辑标记

#### Scenario: Edit disabled

- **GIVEN** 组织不允许编辑
- **WHEN** 发送者尝试编辑
- **THEN** 系统拒绝

### Requirement: Admin IM product settings API

系统 MUST 提供 `/admin-api/im/` 下的产品设置读写接口，并按管理端 RBAC permission 保护。写操作 MUST 校验规则集主体属于当前租户。

#### Scenario: Unauthorized admin API

- **WHEN** 无权限管理员调用更新接口
- **THEN** 系统拒绝（与既有 `@PreAuthorize` 语义一致）

### Requirement: Effective settings for workspace

工作台 MUST 能获取当前租户生效的 IM 组织策略（用于菜单显隐等），但强制执行 MUST 以后端写路径校验为准。

#### Scenario: Client reads effective settings

- **WHEN** 已认证成员请求生效策略
- **THEN** 返回与强制执行一致的撤回/编辑/公开群等开关与时间窗摘要

