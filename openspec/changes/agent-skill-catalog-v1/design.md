## Context

Skill = 产品；Tool = Spring AI；实现 = *-api。

## Goals / Non-Goals

**Goals:** 目录 + 绑定 + 首批内置 Tool。

**Non-Goals:** MCP、市场、对外 Skill HTTP。

## Decisions

### D1 — 用户身份执行
Tool 使用当前用户授权上下文。

### D2 — 白名单
AgentDef.skillIds → Builder allowlist（grok 同构）。

## Risks / Trade-offs

[工具幻觉调不存在的 API] → 严格 schema + 少而精首批

## Open Questions

- 首批 Skill 清单（任务/日历/文档？）
