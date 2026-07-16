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

The system MUST maintain a platform-level bot catalog (not owned by a tenant) and record enablement at tenant and user layers. Bots MUST NOT be persisted as `sys_user` rows. Seeded platform assistants (`org-assistant`, `task-bot`, `approval-bot`, etc.) MUST be stored as `type=system`.

#### Scenario: Bot is not a login user

- **GIVEN** platform seed inserts a bot definition
- **WHEN** querying `sys_user`
- **THEN** no login account row exists for that bot

#### Scenario: System bot catalog for organization reach

- **GIVEN** seeded system bots
- **WHEN** a caller sends an organization invite reminder
- **THEN** the caller uses `botCode=org-assistant`
- **AND** MUST NOT use retired `invite-helper`

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

### Requirement: Card content placeholder

The message content model MUST reserve a `card` shape (and future interactive `actions`) for Feishu-like interactive cards. Foundation MAY allow text plus deep-link metadata first. Full interactive callback auth/timeout/idempotency belongs to a later slice and MUST NOT resurrect a parallel notify write model.

#### Scenario: Text reach allowed

- **GIVEN** `ImBotApi.send` with text and optional deep-link metadata
- **WHEN** the message is persisted
- **THEN** clients can render it in `bot_dm` and navigate the deep link

#### Scenario: Card type reserved

- **GIVEN** a sender uses the reserved `card` content type
- **WHEN** the message is persisted
- **THEN** `im_message` succeeds
- **AND** unimplemented interactive callbacks MUST NOT dual-write `infra_notify`

### Requirement: Conversation list includes bot_dm

The app conversation list MUST include the current user's `bot_dm` conversations with unread and last preview.

#### Scenario: List shows bot conversation

- **GIVEN** the user has a `bot_dm` with unread messages
- **WHEN** GET `/app-api/im/conversation/list`
- **THEN** the response includes an item with `type=bot_dm`
- **AND** includes unread count

### 需求：群聊 REST API

系统 MUST 提供用户端群聊 REST：建群、邀请成员、查询群成员；群消息 MUST 复用现有 `conversationId` 消息发送/列表接口。

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

#### 场景：会话列表含群聊

- 给定 用户持有有效 JWT
- 当 请求 `/app-api/im/conversation/list`
- 那么 返回 direct 与 group 会话
- 并且 group 项包含 `memberCount`

#### 场景：群消息展示发送者昵称

- 给定 用户为群成员
- 当 拉取群会话消息列表
- 那么 用户消息包含 `senderNickname` 供前端展示

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
