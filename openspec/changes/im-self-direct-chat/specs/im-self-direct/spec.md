## ADDED Requirements

### Requirement: 允许自己对自己的单聊会话

系统 MUST 允许当前用户与自己创建并使用 DIRECT 会话。该会话 MUST 仅有一名用户成员；`direct_peer_low` 与 `direct_peer_high` MUST 均为该用户 ID。系统 MUST NOT 因 peer 等于自己而拒绝创建或发送文本消息。

#### Scenario: 首次对自己发消息

- **WHEN** 成员以 peerUserId 等于自己的身份发送单聊消息
- **THEN** 系统创建或复用 self-DIRECT 会话并成功落库消息
- **AND** 会话列表可见该会话且 peer 指向本人

#### Scenario: 不重复气泡

- **WHEN** 成员在 self-DIRECT 中发送消息
- **THEN** 消息流中该条仅作为己方消息展示一次
- **AND** MUST NOT 因成员扇出导致同条消息双泡

### Requirement: 自己名片可发消息且无通话入口

工作台个人名片在 self 模式下 MUST 提供「消息」入口，MUST NOT 提供语音/视频入口。

#### Scenario: 从通讯录点自己发消息

- **WHEN** 成员在通讯录打开自己的名片并点击消息
- **THEN** 进入与自己的单聊（或 pending 后首条发送成功创建）
