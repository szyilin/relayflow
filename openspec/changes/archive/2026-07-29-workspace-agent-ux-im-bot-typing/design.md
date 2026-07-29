# 设计：工作台智能体 UX + IM Bot 正在输入

## Context

- 智能体页当前把「历史任务」与智能体列表互斥显示在侧栏；落地页用独立 USelect 选智能体。
- Runtime 仅 `stream().content()`，丢失 Spring AI 已映射的 `reasoningContent`。
- IM typing 仅人→人；`bot_dm` 明确未覆盖。BotRuntime 同步调 Agent 后一次性 `sendBotReply`。

## Decisions

### D1 — 侧栏：导航 + 常驻历史

上区：新任务 / 智能体 / 自动化（去掉历史任务）。下区始终列 Session，按 `updateTime` 分组（今天 / 昨天 / 更早，ChatGPT 风格）。点「智能体」「自动化」时 **右侧主区** 展示管理 UI，侧栏历史仍在。

### D2 — Prompt 内选智能体

`UChatPrompt` `#footer` 放 `USelect`（智能体列表）+ Submit。会话中切换调用 `POST /app-api/agent/session/switch-agent`，只改 `agent_session.agent_id`，消息历史不变；下一 turn 用新 agent 的 system/model/skills。

### D3 — 流式 Markdown + 等待态

流式阶段 `Comark :streaming="true"` 实时渲染。`streamStatus` 在 `message.user` 时保持 `submitted`，直到首个 `text-delta` / `reasoning-delta` / `tool-start` 才进 `streaming`，以保留 Nuxt UI 三点指示器。

### D4 — Reasoning（Spring AI 已适配则透出）

改用 `spec.stream().chatResponse()`；从 `AssistantMessage` metadata `reasoningContent` 发 `reasoning-delta`；正文仍 `text-delta`。无 reasoning 的模型不发事件。不做逐厂商私有协议适配。

### D5 — Bot typing 属 IM 能力

`ImBotApi.setTyping(ImBotTypingCommand)`：`botCode` + `conversationId` + `typing`。服务端校验 bot 与会话关系后 Redis TTL + `typing.updated` fanout（payload 含 `botId`、`senderType=bot`）。`BotRuntime` 在 platform/carrier 处理前 `true`、finally `false`。前端 typing store 识别 bot，bot_dm 显示「对方正在输入…」。

## Risks

- 兼容网关若不返回 reasoningContent → 仅无 Thinking UI（可接受）
- Bot typing TTL 5s：长推理需 Runtime 心跳续期（实现：处理期间每 3s renew 或拉长 bot TTL 至 60s）

## Migration

无破坏性表变更；新增 REST + ImBotApi 方法。
