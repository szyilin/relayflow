## Context

前端优先；对标飞书 aily 壳层（新任务 / 智能体 / 历史任务 + 主区对话）。
契约真源已由 `agent-runtime-core-v1` 定稿：`openspec/lanes/workspace-agent/contract.md`。

## Goals / Non-Goals

**Goals**

- Rail 入口 `/app/agents`
- 左栏：新任务、智能体列表/编辑、历史会话
- 主区：新任务落地 + Session 聊天（Mock）
- `api/app/agent.ts` 按 contract 预埋；store 临时 Mock

**Non-Goals**

- 调用真 API / Runtime（属 `workspace-agent-integrate`）
- 会话写入或展示在 `/app/messages`
- Skill / Bot 推送 / 自动化

## Decisions

### D1 — 路由

`/app/agents`（`pages/app/agents/index.vue`），`layout: workspace`。

### D2 — 导航

`useWorkspaceNav` EXTRA_RAIL：`agents`，顺序在 tasks 与 docs 之间。

### D3 — Mock 仅 store

`stores/agent.ts` 内 `useMock=true`；禁止页面 `import mocks/`。

### D4 — Session ≠ IM

UI 文案与结构明确会话仅在本页；不触碰 IM store。

## Risks / Trade-offs

[Mock 与 API 形状漂移] → 以 lane contract 为真源；integrate 换 store 实现。

## Open Questions

- 一期不做自动化入口（灰态留给 `agent-automation-v1`）。
