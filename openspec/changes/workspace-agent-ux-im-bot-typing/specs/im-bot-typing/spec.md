## ADDED Requirements

### Requirement: Bot typing indicator API

系统 MUST 在 `ImBotApi` 提供 Bot「正在输入」能力：按 `botCode` + `conversationId` 设置/清除 ephemeral typing，并经 WebSocket `domain=presence` `typing.updated` fanout 给会话内用户成员。MUST NOT 要求 Agent 直连 Redis 或自建协议；Agent/BotRuntime MUST 调用 IM 该能力。IM 消息仍为一整条发送，MUST NOT 因 typing 改为流式消息。

#### Scenario: Carrier bot starts processing

- **WHEN** BotRuntime 开始处理 bot_dm 或群内 @Bot 入站且将调用 Agent/Handler
- **THEN** 调用 `ImBotApi.setTyping(..., typing=true)`，会话对端可见正在输入
- **AND** 处理结束（成功或失败）后 MUST `typing=false`

#### Scenario: Fanout payload identifies bot

- **WHEN** Bot typing 状态变更
- **THEN** `typing.updated` payload 含 `conversationId`、`botId`、`senderType=bot`、`typing`
- **AND** 前端 bot_dm 展示「对方正在输入…」（或等价文案）
