# 提案：工作台智能体 UX + IM Bot 正在输入

## Why

智能体页侧栏/Prompt/流式 Markdown 体验与 Nuxt UI Chat 目标态不一致；IM 中 Carrier Bot 长时间生成时用户只见「发送中」、看不到机器人「正在输入」。需按「IM 规定能力、Agent 适配 IM」完成 Bot typing，并改进智能体工作台壳层。

## What Changes

- 工作台智能体：侧栏常驻历史（按时间分组，去掉「历史任务」入口）；智能体/自动化管理在右侧主区
- ChatPrompt：智能体选择放进 Prompt footer（可在会话中切换智能体，Session 上下文保留）
- 流式 Markdown 实时渲染；`submitted` 指示器保持到首个内容 delta
- Spring AI：用 `chatResponse` 流透出兼容服务的 `reasoningContent` → `reasoning-delta`（无则跳过，不做逐模型 hack）
- IM：`ImBotApi` 增加 Bot typing；BotRuntime 处理前后调用；前端 `bot_dm`/群 Bot 展示正在输入

## Capabilities

### New Capabilities

- `im-bot-typing`：机器人会话级正在输入（IM API + WS fanout）

### Modified Capabilities

- `workspace-agent-chat-ui`：壳层、Prompt、流式 Markdown、reasoning 展示
- `workspace-agent-chat-stream`：runtime 透出 reasoning（best-effort）

## Impact

- `web/src/pages/app/agents/`、`AgentChatPanel`、agent store
- `SpringAiAgentRuntime`、session switch-agent API
- `ImBotApi` / BotRuntime / typing store / messages header
