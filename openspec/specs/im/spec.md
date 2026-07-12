# 即时通讯模块规格（im）

## 目的

定义单聊、群聊、频道及消息持久化相关行为。

## 需求

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

系统必须使用结构化 `content_json`（Content Block 数组）存储消息内容；V1 须支持 `text` 与 `file` 块类型，并预留 `link`、`actions`、`mention` 扩展。

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

系统必须记录消息发布者类型 `sender_type`（`user` | `system` | `bot` | `app`）；V1 须实现 `user` 与 `system`，其余类型预留。

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

#### 场景：系统消息发送

- 给定 模块 A 需向会话投递系统消息
- 当 调用 `ImMessageApi.sendSystemMessage(conversationId, content, publisher)`
- 那么 消息持久化至 `im_message`
- 并且 在线成员经 WebSocket 收到 `domain=im, type=message.new`

### 需求：域事件发布入口

系统必须在平台层提供 `RealtimeEventPublisher` 入口；V1 对 `domain=notify` 与 `domain=presence` 可为占位实现，但接口与 envelope `domain` 枚举必须存在。

#### 场景：非 IM 模块发布事件

- 给定 `bpm-biz` 依赖 `infra-api` 中的 `RealtimeEventPublisher`
- 当 发布 `domain=notify` 事件
- 那么 调用不得抛出「接口不存在」类错误
- 并且 V1 允许 no-op 实现

#### 场景：IM 不得绕过传输层

- 给定 IM 业务需向用户推送 envelope
- 当 消息已持久化
- 那么 须通过 `RealtimeTransportApi` 投递
- 并且 不得在 `im-biz` 内直接持有 WebSocket Session 引用

### 需求：通知中心 API 占位

系统必须预留 `NotifyInboxApi` 接口用于将来审批、任务等通知投递；V1 不得要求实现通知中心表或 UI。

#### 场景：V1 调用通知 API

- 给定 V1 尚未实现通知中心
- 当 模块调用 `NotifyInboxApi.push(...)`
- 那么 系统 MAY 抛出「未实现」或使用 documented no-op
- 并且 不得将通知内容写入 `im_message`
