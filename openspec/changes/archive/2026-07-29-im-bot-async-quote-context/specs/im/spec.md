## ADDED Requirements

### Requirement: Async bot inbound dispatch after message persist

系统 MUST 在用户消息成功持久化并完成会话内实时推送后，立即返回发送 API 响应。Bot 入站（bot_dm 与群 @Bot）MUST 在事务提交后**异步**执行，MUST NOT 阻塞发送 HTTP。等待 Bot 处理期间，用户侧等待态 MUST 依赖 typing 指示，MUST NOT 依赖发送请求挂起。

#### Scenario: Send returns before agent finishes

- **WHEN** 用户向 carrier bot_dm 发送文本消息
- **THEN** 发送 API 在消息落库与实时推送后返回成功
- **AND** Bot/Agent 生成可在返回之后完成
- **AND** 处理期间可对会话发出 bot typing

#### Scenario: Bot failure does not roll back user message

- **WHEN** 异步 Bot 处理失败
- **THEN** 用户消息仍保持已发送
- **AND** 系统 MAY 发送一条 bot 错误提示消息或仅记日志

### Requirement: Single-message WeChat-style quote (reply_to)

系统 MUST 支持单条消息引用：发送请求可带 `replyToMsgId`；持久化写入 `im_message.reply_to_msg_id`；列表与实时推送 MUST 返回引用摘要（被引发送者显示名 + 文本预览截断），供气泡内嵌引用块展示。被引消息 MUST 属于同一会话。V1 MUST NOT 支持多选引用。引用 UI MUST 为微信式引用块，MUST NOT 用 `@` 语法表达本能力。

#### Scenario: Send with reply_to

- **WHEN** 用户对同会话消息 A 发起引用并发送新消息
- **THEN** 新消息 `reply_to_msg_id=A.id`
- **AND** 客户端气泡展示引用块（发送者名 + 预览）
- **AND** 输入区在发送前可取消引用预览

#### Scenario: Reject cross-conversation reply_to

- **WHEN** `replyToMsgId` 指向其他会话消息
- **THEN** 发送失败或忽略该引用（实现须明确一种；推荐失败）

### Requirement: Carrier inbound short history and mention window

对 carrier bot 入站，系统 MUST 在调用 Agent 前装配上下文：

- **bot_dm**：除当前触发句外，注入该会话最近 N 条消息作为短期历史（N 可配置，建议默认 20，硬顶 ≤50），并受字符/token 预算截断。
- **群 @Bot**：除当前触发句外，注入触发消息前后各 M 条（建议 M=5），并受硬顶约束。

若触发消息含 `reply_to_msg_id`，入站文本或结构化上下文 MUST 包含引用摘要（messageId + 发送者 + 预览）。默认历史 MUST 经 IM 读能力装配，MUST NOT 由 Agent 直接查表。

#### Scenario: bot_dm receives recent history

- **WHEN** 用户在 bot_dm 连续对话后发送新消息
- **THEN** Agent 入站 history/上下文含最近若干条（非空，除非会话仅有该条）
- **AND** MUST NOT 默认注入全量历史

#### Scenario: group @Bot receives nearby window

- **WHEN** 用户在群内 @ carrier bot
- **THEN** 入站上下文含触发句附近窗口（前后各 M 条量级）
- **AND** 系统提示或工具描述引导：更早内容须调用会话内读工具

## MODIFIED Requirements

### Requirement: Carrier inbound to AgentRuntime

Inbound user messages to a carrier bot (group @mention of a member bot, or bot_dm) SHALL be dispatched to the Agent runtime under the triggering user's security context. The assistant reply SHALL be written as an IM bot message in the same conversation. Those turns MUST NOT be written to `agent_session`. Dispatch MUST be asynchronous relative to the user send HTTP response. The inbound command MUST include short conversation context as specified by carrier short history / mention window rules, not an empty history by default when prior messages exist.

#### Scenario: bot_dm inbound replies in IM

- **WHEN** a user sends a text message in a bot_dm with a carrier bot
- **THEN** the send API returns after persist without waiting for Agent completion
- **AND** the Agent runtime may produce a reply asynchronously
- **AND** the reply is persisted via bot reply in that conversation
- **AND** no `agent_session_message` row is created for that turn
- **AND** when prior messages exist in that bot_dm, Agent receives non-empty short history
