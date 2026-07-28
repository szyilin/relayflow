## Context

前端优先；对标 aily 壳层（新任务/历史/智能体）。

## Goals / Non-Goals

**Goals:** 可演示 UI + contract 草案。

**Non-Goals:** 真 API、消息列表混入 Session。

## Decisions

### D1 — 独立路由区
`/app/agents` 或同等（深化定）；不进 messages。

### D2 — Mock 仅 store 内
禁止复活全局 mocks 目录约定。

## Risks / Trade-offs

[UI 与 API 漂移] → lane contract 为真源

## Open Questions

- 导航放 Rail 何处？
- 是否一期做自动化入口灰态？
