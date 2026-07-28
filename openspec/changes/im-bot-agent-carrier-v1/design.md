## Context

IM 已有 Bot Runtime；扩展 tenant Bot 供给与 Agent 绑定。

## Goals / Non-Goals

**Goals:** Provisioning API + 生命周期 + 入站转 AgentRuntime。

**Non-Goals:** 开放平台 webhook、Automation UI。

## Decisions

### D1 — 供给 API 在 im-api
Agent 不直写 im_*。

### D2 — 入站不写 AgentSession（默认）
群 @ / bot_dm 对话结果回 IM；与 agent_web 分离（OpenClaw 通道确定性）。

## Risks / Trade-offs

[Bot 孤儿] → 删除 Agent 必须 retire

## Open Questions

- 一 Agent 是否严格 1:1 Bot？
