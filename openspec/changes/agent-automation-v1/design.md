## Context

aily 自动化：触发 → Agent → Bot 推送。

## Goals / Non-Goals

**Goals:** cron 自动化 + 推送目标；结果进 IM。

**Non-Goals:** 闲时队列、NL 推送条件（可后置）、多智能体。

## Decisions

### D1 — 调度用 XXL-JOB
与仓库约定一致。

### D2 — 结果不进 AgentSession
只走 ImBotApi。

## Risks / Trade-offs

[与 Session 混淆] → UI/文案隔离

## Open Questions

- 本 change 是否含 `-web` 还是另拆？
