## 1. IM Bot typing

- [x] 1.1 `ImBotApi` + DTO：`setTyping(botCode, conversationId, typing)`；实现 Redis TTL + `typing.updated`（`botId`/`senderType=bot`）
- [x] 1.2 `BotRuntime`：platform/carrier 处理前 typing=true，finally false（长任务可续期）
- [x] 1.3 前端 typing store / WS：识别 bot typing；bot_dm 头栏「对方正在输入…」

## 2. Agent runtime reasoning

- [x] 2.1 `SpringAiAgentRuntime`：`stream().chatResponse()` 映射 reasoningContent → reasoning-delta，正文 → text-delta

## 3. Session switch agent

- [x] 3.1 `POST /app-api/agent/session/switch-agent`；前端 store + Prompt 内切换

## 4. 工作台 UX

- [x] 4.1 侧栏：去掉历史任务；常驻历史 + 时间分组；智能体/自动化进右侧
- [x] 4.2 ChatPrompt footer 选智能体；去掉落地页独立 Select
- [x] 4.3 流式 Comark streaming；submitted 保持到首个内容 delta
- [x] 4.4 `pnpm build` + `pnpm typecheck`；后端 `./mvnw -pl relayflow-server -am compile`
