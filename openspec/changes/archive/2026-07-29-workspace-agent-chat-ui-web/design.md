## Context

`/app/agents` 已联调同步 `POST /app-api/agent/session/send`，消息仅 `content: string`，气泡手写。Nuxt UI 4.10+ 提供 AI Chat 组件族，官方示例绑定 Vercel AI SDK `UIMessage.parts` + Node `streamText`；RelayFlow 推理在 **Spring AI**，不能照搬 AI Gateway。本 change 只做 **web lane**：组件换装、parts 模型、Mock 流式、contract 草案。

## Goals / Non-Goals

**Goals:**

- 对话区使用 `UChatMessages`、`UChatMessage`、`UChatPrompt`、`UChatPromptSubmit`、`UChatReasoning`、`UChatTool`、`UChatShimmer`
- 助手回复用 `Comark` 渲染 Markdown（流式友好）
- 前端统一 `parts[]`（text / reasoning / tool）；历史将现有 `content` 映射为 text part
- Mock SSE（store 内）演示 reasoning → tool → text-delta；`pnpm build` + `typecheck` 通过
- 起草 `openspec/lanes/workspace-agent-chat/contract.md`

**Non-Goals:**

- 真实 Spring AI SSE / `parts_json` 落库（→ `workspace-agent-chat-stream-api`）
- Tool approval 交互、ChatPalette 弹层
- IM 正在输入、侧栏拖拽排序
- 废弃同步 `/send`（integrate 前仍可作兜底）

## Decisions

### D1 — 不用 Node AI Gateway；自研事件 → parts

- **选择**：SSE 事件（后续 API）与 Mock 事件同构：`reasoning-delta` / `tool-start` / `tool-result` / `text-delta` / `done` / `error`；前端映射为接近 AI SDK 的 parts，再喂给 `UChat*`
- **替代**：完整 `useChat` + AI SDK data stream protocol —— 与 Spring Security JWT、现有 Pinia session 摩擦大；本期不强制 `useChat` 接管会话列表

### D2 — 依赖

- `@nuxt/ui` → `^4.10.0`
- `ai`、`@ai-sdk/vue`（类型/工具函数如 `isTextUIPart`；不强制 DefaultChatTransport）
- `@comark/vue`；Vite `ui({ prose: true })`

### D3 — Mock 与真实 send 并存

- `-web`：`sendMessage` 走 Mock stream（含假 reasoning/tool + Markdown 文本）；可选仍调同步 send 把真实回复写入最终 text part（优先：**Mock 完成后若 sync 成功则用真实 content 覆盖 text**，保证联调期仍可对话）
- `import.meta.env` 或 store 开关 `useMockAgentStream`，integrate 删除 Mock

### D4 — 组件拆分

- 新增 `components/workspace/agent/AgentChatPanel.vue`（消息列表 + prompt）
- `index.vue` 保留侧栏、Agent/自动化编辑；对话主区改为挂载 ChatPanel

### D5 — Contract 预告 API

- `POST /app-api/agent/session/send/stream`：`text/event-stream`，事件 JSON；结束落库 `content` + `parts_json`
- 历史 `GET /messages` 后续返回 `parts`；本期前端兼容仅 `content`

## Risks / Trade-offs

- [升级 Nuxt UI 4.10] → 回归管理端/工作台关键组件；缓解：升级后全量 `pnpm build` + `typecheck`
- [Mock 与真实回复不一致] → 覆盖 text part；标明 integrate 换真实 SSE
- [模型无 reasoning] → UI 有 Reasoning 组件但不伪造；Mock 仅演示

## Migration Plan

1. 合入 web 后 UI 立即可用（Mock stream）
2. 后续 `-api` 按 contract 实现；`-integrate` 去 Mock
3. 回滚：还原 agents 页气泡 + 降级依赖（无 DB 变更）

## Open Questions

- Tool approval 是否在 stream-api 一并做？（建议 V1.1）
- `parts_json` 列 vs 独立 tool_trace 表？（建议先 JSONB 列）
