## ADDED Requirements

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

## MODIFIED Requirements

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
