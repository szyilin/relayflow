## Why

将 `workspace-agent-web` 的 Mock 去掉，对接 `agent-runtime-core-v1` 的真实 Session/Runtime API，完成可演示的端到端「选智能体 → 对话」。

## What Changes

- Store 只走真实 `/app-api/agent/**`
- 联调流式/非流式（以 runtime contract 为准）
- 看板与 validate；浏览器路径验收

## Capabilities

### Modified Capabilities

- `agent`：补齐联调相关场景（若 web/runtime delta 有缺口）

## Impact

- `web/` + 已存在 agent API；依赖 web + runtime-core
- 无新 Maven 模块

## 非目标

- Skill 全量、Automation、Carrier Bot
