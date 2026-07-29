## Context

IM 已有 Bot Runtime（群 @ → BotIngress → BotRuntime）；系统 Bot 仅 Flyway 种子。
AgentDef 已有 `carrier_bot_id` 列但未接线。本切片落地 **tenant Carrier Bot 供给** 与入站转发 AgentRuntime。

## Goals / Non-Goals

**Goals**

- `ImBotProvisioningApi`：按 agentId 幂等 provision / retire / update profile
- `type=tenant`、`handler_kind=platform`、`owner_agent_id` 绑定；1 Agent ↔ 1 Bot
- Agent 创建→provision，删除→retire
- 入站（群 @ + bot_dm）→ `AgentCarrierInboundApi` → AgentRuntime → `sendBotReply`
- **不写** `agent_session`

**Non-Goals**

- 开放平台 webhook、Automation UI、Agent 工作台技能勾选 UI

## Decisions

### D1 — 供给 API 在 im-api

Agent 只调 `ImBotProvisioningApi`，不写 `im_*`。

### D2 — 入站不写 AgentSession

IM 通道回合仅回 IM；与 `agent_web` Session 分离。

### D3 — 1:1 Carrier

`im_bot.owner_agent_id` 唯一（未删除行）；code = `agent-carrier-{agentId}`。

### D4 — 跨域入站契约

`agent-api.AgentCarrierInboundApi.handleInbound(...)` → reply text；
`im-biz.BotRuntime` 在 `owner_agent_id != null` 时调用该 API（否则沿用 `BotPlatformHandlerRegistry`）。

### D5 — Retire

`status=0` + 软删 enablement；保留 bot 行便于幂等再 provision 时复活。

## Risks / Trade-offs

[Bot 孤儿] → Agent delete 必须 retire  
[LLM 慢] → afterCommit 异步同线程仍阻塞用户请求尾部；V1 可接受，流式/异步另开

## Open Questions

- 无（1:1 已拍板）
