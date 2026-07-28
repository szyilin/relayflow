# Tasks：agent-skill-catalog-v1

## 0. 深化

- [x] 0.1 冻结首批 Skill：`task.search`、`task.list_due`（schema 见 design D1）
- [x] 0.2 补全 specs/tasks/design

## 1. 实现

- [x] 1.1 SkillRegistry + 元数据 + catalog API
- [x] 1.2 Builtin Tool：`task.search` / `task.list_due`（`TaskItemApi`）
- [x] 1.3 AgentDef `skillIds` 读写；Session send 经 Registry → Builder
- [x] 1.4 测试 + compile
