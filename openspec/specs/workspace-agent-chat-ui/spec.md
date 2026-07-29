# workspace-agent-chat-ui Specification

## Purpose
TBD - created by archiving change workspace-agent-chat-ui-web. Update Purpose after archive.
## Requirements
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

### Requirement: Workspace agent chat uses real SSE stream

工作台智能体会话发送 MUST 使用 `POST /app-api/agent/session/send/stream`（`text/event-stream`），按 lane contract 消费 `message.user`、`text-delta`（及可选 reasoning/tool）、`message.assistant`、`done` / `error`。前端 MUST NOT 再依赖 Mock 流式 preamble 或「Mock + 同步 `/send` 覆盖」双轨作为主路径。

#### Scenario: User sends a message in an open session

- **WHEN** 用户在已打开会话中提交 Prompt
- **THEN** 客户端建立 SSE 流，增量更新助手 `parts`，结束后以服务端 `message.assistant` 为准刷新该条消息（含 `id` / `parts` / `content`）

#### Scenario: User stops generation

- **WHEN** 用户点击停止
- **THEN** 客户端 abort 请求；服务端尽量停止生成；已落库的用户消息仍可在历史中看到

#### Scenario: History replay with parts

- **WHEN** 用户打开历史会话且消息带 `parts`
- **THEN** UI 按 `parts` 渲染 Reasoning / Tool / Markdown text；无 `parts` 时将 `content` 映射为单一 text part

### Requirement: Stream API contract draft

本 change MUST 在 `openspec/lanes/workspace-agent-chat/contract.md` 起草流式发送契约（路径、鉴权、SSE 事件类型、与 `parts` 落库意图），供后续 `-api` 实现。

#### Scenario: Contract lists stream endpoint

- **WHEN** 后端工程师打开该 contract
- **THEN** 可见 `POST /app-api/agent/session/send/stream`（或等价路径）、事件类型列表与鉴权说明

