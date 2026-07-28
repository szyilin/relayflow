## Why

技能是智能体操作企业系统的关键：产品面「技能」、运行时 Tool、实现调 `*-api`。本切片建立 Skill 目录与首批内置能力（如任务/日程查询），挂到 AgentDef 与 Spring AI Tool Calling。

## What Changes

- SkillCatalog（code、名称、描述、input schema、权限范围）
- Builtin Skill → `ToolCallback`；调用 `task-api` / `calendar-api` 等
- Agent 绑定 skillIds；Builder 按白名单装配（grok tools allowlist 同构）
- **不做**对外 Skill HTTP OpenAPI（后置）

## Capabilities

### Modified Capabilities

- `agent`：技能目录与绑定、Tool 执行身份（当前用户）

## Impact

- `agent-biz` skill 包；依赖 runtime-core；跨域仅 `*-api`
- 参照：飞书 Aily「技能」产品名 + OpenClaw/Spring AI tool 形态

## 非目标

- MCP、技能市场、Workflow 可视化编排
