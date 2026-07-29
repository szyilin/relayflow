## Context

Skill = 产品名；Tool = Spring AI `ToolCallback`；实现只调 `*-api`（当前用户身份）。
`agent_def.skill_ids_json` 已存在，此前未接线。

## Goals / Non-Goals

**Goals**

- SkillRegistry + 可列举元数据
- 首批 2 个内置 Skill：`task.search`、`task.list_due`（`TaskItemApi`）
- AgentDef `skillIds` 读写；Session send → Builder allowlist → Runtime toolCallbacks
- 单元测试 + compile

**Non-Goals**

- 日历 / 文档 Skill（calendar/docs 尚无只读 `*-api`，另开）
- MCP、技能市场、对外 Skill HTTP、前端技能选择 UI（可后置；API 已暴露 skillIds）

## Decisions

### D1 — 首批 Skill 冻结

| code | 工具名 | 能力 | 实现 |
|------|--------|------|------|
| `task.search` | `task_search` | 按关键字搜索「我的」任务 | `TaskItemApi.searchTasks` |
| `task.list_due` | `task_list_due` | 按截止时间区间列「我的」待办 | `TaskItemApi.listDueRange` |

工具名将 `.` 换成 `_`（兼容 OpenAI function 命名）。

### D2 — 用户身份执行

Tool 在请求线程读取 `TenantContextHolder` + `SecurityFrameworkUtils`，把 `tenantId`/`userId` 显式传给 `*-api`。

### D3 — 白名单

`skill_ids_json` → 仅解析已注册 code → `ToolCallback` 列表；未知 code 在保存时拒绝。

### D4 — 目录 API

`GET /app-api/agent/skill/catalog` 返回元数据（code/name/description），供后续 UI 勾选。

## Risks / Trade-offs

[工具幻觉] → 少而精 + 严格 inputType  
[日历延后] → 等 calendar-api 只读方法后再加 Skill

## Open Questions

- 无（日历/文档延后已关闭）
