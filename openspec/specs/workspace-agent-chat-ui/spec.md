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

### Requirement: Workspace agent chat shell and prompt

工作台智能体侧栏 MUST 常驻展示历史 Session（按最近活动时间分组），MUST NOT 用「历史任务」顶栏入口切换替换该列表。顶栏仅保留新任务 / 智能体 / 自动化；选中智能体或自动化时，管理功能 MUST 显示在右侧主区。发送 Prompt MUST 使用 `UChatPrompt`，智能体选择 MUST 置于 Prompt footer（禁止落地页独立大块 Select）。同一 Session 内 MUST 允许切换智能体且保留消息上下文。

#### Scenario: History always visible

- **WHEN** 用户打开智能体页并切换「智能体」或「自动化」
- **THEN** 侧栏下方仍列出历史对话
- **AND** 右侧主区展示对应管理 UI

#### Scenario: Switch agent mid-session

- **WHEN** 用户在已有 Session 的 Prompt footer 选择另一智能体并继续发送
- **THEN** Session 绑定的 agentId 更新，历史消息仍在该 Session
- **AND** 后续生成使用新智能体的模型与人设

### Requirement: Streaming markdown and waiting indicator

流式正文 MUST 在 delta 到达过程中以 Markdown 方式增量渲染（`Comark` streaming）。从提交到首个内容 delta 期间 MUST 保持等待指示（Nuxt UI Chat submitted/indicator），避免长时间空白。

#### Scenario: User waits for first token

- **WHEN** 用户发送消息且服务端尚未产出 text/reasoning/tool delta
- **THEN** UI 显示 submitted/streaming 等待指示

#### Scenario: Markdown during stream

- **WHEN** 助手 `text-delta` 持续到达
- **THEN** 助手气泡内 Markdown 随增量更新，不必等 `done`
