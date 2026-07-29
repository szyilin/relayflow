## Context

联调 `workspace-agent-web` ↔ `agent-runtime-core-v1`。契约真源：`openspec/lanes/workspace-agent/contract.md`。

## Goals / Non-Goals

**Goals**

- 去掉 `stores/agent.ts` 临时 Mock / seed
- Store 只走 `/app-api/agent/def|session|llm/**`
- 端到端：建智能体 → 新任务 → 发送 → Runtime 回复；Session 不进 IM

**Non-Goals**

- Skill / Automation / Carrier Bot
- 流式输出（本期非流式 `send` 闭环；流式另开）

## Decisions

### D1 — contract 已对齐

web `api/app/agent.ts` 与 runtime-core controller/VO 一致；ID 按框架 `Long → string` 规范化。

### D2 — 非流式闭环

`POST /session/send` 一次返回 user + assistant；UI 追加两条。

### D3 — 模型依赖管理端

创建智能体前须有 `/app-api/agent/llm/model/list-enabled`；无模型时 UI 提示去 `/admin/infra/llm`。

## Risks / Trade-offs

[LLM 调用慢/失败] → toast + `lastError`；不写 IM。

## Open Questions

- 无
