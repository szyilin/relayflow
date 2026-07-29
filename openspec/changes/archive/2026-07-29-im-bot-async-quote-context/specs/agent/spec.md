## ADDED Requirements

### Requirement: Carrier inbound uses short IM history

`AgentCarrierInboundApi`（或等价入站实现）MUST 接受并使用由 IM 侧装配的短期历史与可选引用摘要；调用 `AgentRuntime` 时 MUST 将短期历史映射为 runtime `history`（或等价多轮消息），MUST NOT 在存在可读近期消息时固定传入空 history。入站 MUST 在触发用户安全上下文下运行。

#### Scenario: Non-empty history passed to runtime

- **WHEN** carrier 入站命令携带近期消息列表
- **THEN** `AgentRuntime.run` 的 history 含对应轮次
- **AND** 当前用户句为最新 user message（可含引用说明）

### Requirement: Agent im.* skills call ImConversationReadApi only

系统 MUST 提供 BuiltinSkill（建议 code：`im.read_message`、`im.read_nearby`、`im.read_history`）供 Agent 在需要时读取**当前触发会话**更多消息。技能实现 MUST 仅依赖 `ImConversationReadApi`（im-api），MUST NOT 依赖 im-biz 或直查 `im_*` 表。执行 MUST 绑定入站上下文中的 `conversationId`（及触发用户/租户）；模型传入的其他会话 ID MUST 被拒绝。

#### Scenario: Tool reads current conversation only

- **WHEN** Agent 调用 `im.read_history` 且上下文绑定会话 C
- **THEN** 仅返回 C 内消息（受上限约束）

#### Scenario: Tool rejects other conversationId

- **WHEN** 模型入参指定会话 D ≠ 绑定会话 C
- **THEN** 工具失败或拒绝，MUST NOT 返回 D 的内容

#### Scenario: Skill mirrors task.list_due layering

- **WHEN** 实现 `im.*` 技能
- **THEN** 结构为 Agent 薄适配器 → `ImConversationReadApi`，与现有 `task.list_due` → `TaskItemApi` 模式一致
