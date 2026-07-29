## MODIFIED Requirements

### Requirement: Stream reasoning when Spring AI provides it

`SpringAiAgentRuntime` MUST 使用 ChatResponse 流（非仅 `.content()`），并在 AssistantMessage metadata 存在非空 `reasoningContent` 时发出 `reasoning-delta`（及适当时 `reasoning-end`）。当模型/网关不提供 reasoning 时 MUST 仅发 text-delta，MUST NOT 为各厂商私有协议单独硬编码适配层。

#### Scenario: Compatible reasoning model

- **WHEN** OpenAI 兼容服务在流式块中返回 `reasoning_content`
- **THEN** 客户端收到 `reasoning-delta` 事件并可渲染 `UChatReasoning`

#### Scenario: Plain chat model

- **WHEN** 模型不返回 reasoningContent
- **THEN** 流仅含 text-delta（及既有 tool 事件若有），行为与先前一致
