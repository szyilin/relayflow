## ADDED Requirements

### Requirement: Agent chat uses Nuxt UI AI Chat components

工作台 `/app/agents` 活动会话的对话区 MUST 使用 Nuxt UI AI Chat 组件族渲染消息列表与输入区，包括 `UChatMessages`（或等价消息列表容器）、`UChatPrompt`、`UChatPromptSubmit`，并按 parts 类型渲染 `UChatReasoning`、`UChatTool` 与流式 Markdown（`Comark`）。

#### Scenario: Session view shows Chat shell

- **WHEN** 用户打开已有智能体会话
- **THEN** 主区展示基于 Nuxt UI Chat 的消息列表与提示输入区，而非仅手写气泡 + 原生 textarea

#### Scenario: Assistant markdown renders

- **WHEN** 助手消息 text part 含 Markdown（如标题、列表、代码围栏）
- **THEN** 系统以流式友好的 Markdown 组件渲染该 part（非纯 `whitespace-pre-wrap` 原文）

### Requirement: Messages are modeled with parts

前端会话消息 MUST 支持 `parts` 数组；至少识别 `text`、`reasoning`、`tool` 三类。仅含 `content` 的历史消息 MUST 映射为单一 text part。

#### Scenario: Legacy content maps to text part

- **WHEN** API 返回的消息只有 `content` 字符串、无 `parts`
- **THEN** UI 将其视为一个 text part 并正常展示

#### Scenario: Reasoning and tool parts display

- **WHEN** 消息含 reasoning part 与 tool part
- **THEN** UI 分别以可折叠的 Reasoning 与 Tool 组件展示，且 tool 可显示进行中（streaming）状态

### Requirement: Streaming UX with temporary mock

在真实 SSE 未接入前，发送消息 MUST 能通过 store 内临时 Mock 流式事件演示 token/块级更新（含可选的 reasoning 与 tool 调用过程）。Mock MUST 可在后续 integrate 删除，且不得引入常驻 `web/src/mocks/` 目录。

#### Scenario: User sends and sees progressive assistant output

- **WHEN** 用户在会话中发送一条消息且 Mock 流式开启
- **THEN** 助手气泡逐步出现 reasoning/tool/text 更新，发送控件进入 streaming/stop 可用状态（或等价 loading）

### Requirement: Stream API contract draft

本 change MUST 在 `openspec/lanes/workspace-agent-chat/contract.md` 起草流式发送契约（路径、鉴权、SSE 事件类型、与 `parts` 落库意图），供后续 `-api` 实现。

#### Scenario: Contract lists stream endpoint

- **WHEN** 后端工程师打开该 contract
- **THEN** 可见 `POST /app-api/agent/session/send/stream`（或等价路径）、事件类型列表与鉴权说明
