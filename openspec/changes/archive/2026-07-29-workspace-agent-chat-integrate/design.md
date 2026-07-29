## Context

- Web：`USE_MOCK_AGENT_STREAM=true`，先 Mock preamble（假 reasoning/tool）再调同步 `/send` 覆盖 text。
- API：`POST /app-api/agent/session/send/stream` 已可用；消息含可选 `parts`；`parts_json` 已落库。
- Contract：`openspec/lanes/workspace-agent-chat/contract.md`。

## Goals / Non-Goals

**Goals:**

- 工作台发送主路径只走真实 SSE
- 删除 Mock 流式助手与双轨 sync 覆盖逻辑
- `pnpm build` + `typecheck`；看板更新

**Non-Goals:**

- Tool approval、ChatPalette
- 废弃同步 `/send`
- IM typing / rail order
- 增强 Spring AI tool/reasoning 映射（stream-api 已尽力；不足时仅 text-delta）

## Decisions

### D1 — fetch + SSE 行解析（不用 EventSource）

- POST + JWT：`fetch` + `ReadableStream`，按行解析 `data: {...}`
- AbortController 对接 `stopStreaming`

### D2 — 事件 → parts 复用现有 `applyStreamEvent`

- 将 SSE JSON 映射到与 Mock 同构的内部事件，或直接扩展 `applyStreamEvent` 支持 contract 事件名
- `message.user` / `message.assistant` 用服务端 id 替换临时消息

### D3 — axios 不承载 SSE

- 新增 `api/app/agent.ts` 中 `sendAgentSessionMessageStream(...)`（基于 `getAccessToken` / baseURL），不走 axios interceptors 的 JSON 假定

## Risks / Trade-offs

- [代理缓冲 SSE] → Vite proxy 需关闭缓冲或确认流式透传
- [重复 done 事件] → runtime 已发 done，session 也发 done；前端幂等忽略第二次
- [取消后用户消息已落库] → 与 contract 一致；UI 保留 user 行，助手临时行可移除或保留半成品

## Migration Plan

1. 合入后刷新 `/app/agents` 即可
2. 回滚：临时恢复 Mock 开关
