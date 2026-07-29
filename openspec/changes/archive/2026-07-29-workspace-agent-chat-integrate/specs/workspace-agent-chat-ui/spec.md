## MODIFIED Requirements

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
