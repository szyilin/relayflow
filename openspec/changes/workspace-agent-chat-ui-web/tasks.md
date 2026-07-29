## 1. Dependencies & tooling

- [x] 1.1 将 `@nuxt/ui` 升级到 `^4.10.0`，添加 `ai`、`@ai-sdk/vue`、`@comark/vue`；`pnpm install`
- [x] 1.2 Vite `ui({ prose: true })`（及暗色代码块 CSS 如需要）；确认 Chat 组件可引用

## 2. Types & store stream

- [x] 2.1 扩展 `AgentSessionMessage`：`parts`（text / reasoning / tool）；`normalizeMessage` 将纯 `content` 映射为 text part
- [x] 2.2 Store：`streamStatus`、Mock 流式发送（reasoning → tool → text-delta）；结束后可选调用现有 sync `/send` 用真实回复覆盖 text；提供 stop
- [x] 2.3 抽出 `mockAgentStream` 助手（仅 store/composables，不进 `web/src/mocks/`）

## 3. Chat UI

- [x] 3.1 新增 `AgentChatPanel.vue`：`UChatMessages` + parts 渲染（`UChatReasoning` / `UChatTool` / `Comark`）+ `UChatPrompt` / `UChatPromptSubmit`
- [x] 3.2 `pages/app/agents/index.vue` 会话主区改用 `AgentChatPanel`；保留侧栏与编辑弹层
- [x] 3.3 空态 / 首页启动对话路径与 Chat Prompt 对齐（可选简化：首页仍选智能体后进会话再 Chat）

## 4. Contract & board

- [x] 4.1 起草 `openspec/lanes/workspace-agent-chat/contract.md`（stream 路径、事件、鉴权、parts 落库意图）
- [x] 4.2 更新 `docs/dev/api-integration-board.md`：本切片 web → in_progress / ui_ready

## 5. Verify

- [x] 5.1 `cd web && pnpm build && pnpm typecheck`
- [x] 5.2 浏览器路径说明：`/app/agents` 开会话发送，可见流式/Reasoning/Tool（Mock）与 Markdown
