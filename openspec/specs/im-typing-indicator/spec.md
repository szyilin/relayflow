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

