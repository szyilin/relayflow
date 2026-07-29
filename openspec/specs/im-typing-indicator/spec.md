# im-typing-indicator Specification

## Purpose
TBD - created by archiving change im-typing-indicator-v1. Update Purpose after archive.
## Requirements
### Requirement: Conversation typing indicator over WebSocket

系统 MUST 支持会话级「正在输入」状态：成员通过 WebSocket `domain=presence` 上报 `typing.start` / `typing.stop`；服务端 MUST 校验其为该会话成员，并将 `typing.updated` fanout 给会话内其他用户（不含上报者）。状态 MUST 为 ephemeral（Redis TTL），MUST NOT 写入业务表。

#### Scenario: Peer sees typing in direct chat

- **WHEN** 用户 A 在私聊会话中开始输入并上报 `typing.start`
- **THEN** 用户 B 收到 `typing.updated`（`typing=true`），消息页标题区展示正在输入提示

#### Scenario: Typing stops on send or idle

- **WHEN** 用户清空输入、失焦、发送消息或上报 `typing.stop`，或 Redis TTL 到期
- **THEN** 对端收到或推断 `typing=false`，标题区恢复原副标题

#### Scenario: Non-member cannot publish typing

- **WHEN** 用户对非成员会话上报 typing
- **THEN** 服务端 MUST 拒绝或静默忽略，且 MUST NOT fanout

### Requirement: Group typing display names

系统在群会话展示 typing 时 MUST 使用正在输入者的显示名（用户昵称或 Bot 名称），例如「张三正在输入…」「助手名正在输入…」；多人时可聚合为「张三等 N 人正在输入…」（文案可微调）。bot_dm 会话 MAY 继续使用「对方正在输入…」。MUST NOT 在群聊一律显示「对方正在输入…」。

#### Scenario: Group shows member name while typing

- **WHEN** 群成员 A 上报 typing，且会话类型为群
- **THEN** 其他成员标题区/副标题看到含 A 显示名的输入提示

#### Scenario: Group shows bot name while bot typing

- **WHEN** 群内 Bot 发出 typing=true
- **THEN** 成员看到含该 Bot 显示名的输入提示
