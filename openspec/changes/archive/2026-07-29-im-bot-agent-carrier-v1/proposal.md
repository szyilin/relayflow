## Why

自定义 Bot 是 Agent 的 IM **载体**：主动推送与群 @ 对话走已有 IM 地基。本切片让 IM 提供通用供给 API，Agent 创建/删除时绑定生命周期；入站经 Bot Runtime 转发 `AgentRuntime`。Agent Session 仍不进消息列表。

## What Changes

- `ImBotProvisioningApi`（或等价）：create/retire/update profile，`type=tenant`，关联 `ownerAgentId`
- Agent 生命周期挂钩（创建→provision，删除→retire）
- `handler_kind=platform`：入站 dispatch 到 AgentRuntime（按 agentId）
- 群可用范围规则沿用现有 Bot 成员模型（深化）

## Capabilities

### Modified Capabilities

- `im`：Carrier Bot 供给 API、与 Agent 绑定
- `agent`：carrierBotId 字段与生命周期

## Impact

- `im-api`/`im-biz` + `agent-biz`；依赖 runtime-core 与现有 Bot Runtime
- 架构：`docs/dev/im-messaging-architecture-draft.md` + agent 草案

## 非目标

- 外部 webhook Bot 开放平台、Automation 完整 UI（见 `agent-automation-v1`）
