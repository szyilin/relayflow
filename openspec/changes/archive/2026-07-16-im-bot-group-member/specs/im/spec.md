## ADDED Requirements

### Requirement: Group bot membership management

The system MUST allow authorized group members to add and remove bots as conversation members with `subject_type=bot`. Member list APIs MUST distinguish bots from users and expose bot display metadata (name/avatar from `im_bot`). Adding an already-present bot MUST be idempotent. Outbound `ImBotApi` MUST NOT depend on group bot membership. The system MUST NOT persist bots as `sys_user` rows.

#### Scenario: Attach bot to group

- **GIVEN** a `group` conversation and a reachable bot for the tenant
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

## MODIFIED Requirements

### Requirement: 群聊 REST API

The system MUST provide workspace group REST for create, invite users, query members, and manage bot membership. Group messages MUST reuse existing `conversationId` send/list APIs.

#### Scenario: 创建群聊

- 给定 已认证的有效组织成员
- 当 POST `/app-api/im/group/create` 提交有效群名与至少一名成员
- 那么 系统创建 `im_conversation(type=group)`、`im_group` 及成员关系（创建者为 owner）
- 并且 返回 `conversationId` 与 `groupId`

#### Scenario: 邀请成员

- 给定 用户为群成员
- 当 POST `/app-api/im/group/members/add` 提交新成员 userId 列表
- 那么 新成员写入 `im_conversation_member`
- 并且 为每次加入事件持久化一条系统消息

#### Scenario: 成员列表含 Bot

- 给定 群内同时有 User 与 Bot 成员
- 当 GET 群成员列表
- 那么 响应区分 `subjectType=user|bot`
- 并且 Bot 项包含可展示的名称（来自 Bot 目录）

#### Scenario: 会话列表含群聊

- 给定 用户持有有效 JWT
- 当 请求 `/app-api/im/conversation/list`
- 那么 返回 direct 与 group 会话
- 并且 group 项包含 `memberCount`

#### Scenario: 群消息展示发送者昵称

- 给定 群会话中存在其他成员发送的消息
- 当 客户端拉取消息列表或收到 `message.new`
- 那么 每条用户消息可解析出发送者展示名
