# Tasks：agent-platform-foundation（母 change）

> **本文件 = 规划收口 + 开子 change 骨架**。禁止在本 change 实现完整智能体产品。  
> 验证：`openspec validate agent-platform-foundation --strict`

## 1. 架构文档与索引

- [x] 1.1 新增 `docs/dev/agent-architecture-draft.md`（拍板结论、分层、Spring AI 落点、开源对照、子 change 表）
- [x] 1.2 更新 `docs/dev/README.md` 增加 Agent 架构草案条目
- [x] 1.3 更新 `AGENTS.md`「下一优先」指向 `agent-platform-foundation` 及子 change 路线（不从 archive 未勾任务续做其它域）
- [x] 1.4 `openspec validate agent-platform-foundation --strict` 通过

## 2. 开子 change 骨架（勾选 = 目录与 proposal/design/tasks 骨架已存在，非实现完成）

> 骨架要求：每个子 change 至少有 `proposal.md` + `design.md`（可含 Open Questions）+ `tasks.md`（高层阶段 +「深化」占位）；不必一次写满全部 delta 场景。深化可另开会话按子 change 补强 design/specs 后再 `/opsx:apply`。

- [x] 2.1 开 `admin-llm-provider-v1`：管理端模型 Provider / Key / 连通性 / 组织启用
- [x] 2.2 开 `agent-module-scaffold`：`module-agent` Maven + BOM Spring AI + 包结构空壳 `[平台]`
- [x] 2.3 开 `agent-runtime-core-v1`：AgentDef + AgentBuilder + Spring AI Runtime + Session API（**亮点核心**）
- [x] 2.4 开 `workspace-agent-web`：工作台智能体壳 + 新任务/历史/聊天 UI（前端优先）
- [x] 2.5 开 `workspace-agent-integrate`：去 Mock 联调 Session/Runtime
- [x] 2.6 开 `agent-skill-catalog-v1`：Skill 目录 + 内置 Tool（调 `*-api`）
- [x] 2.7 开 `im-bot-agent-carrier-v1`：`ImBotProvisioningApi` + Agent 生命周期 + 入站转 Runtime
- [x] 2.8 开 `agent-automation-v1`：定时/触发 + 推送仅走 IM Bot

## 3. 收尾

- [x] 3.1 在 `docs/dev/api-integration-board.md` 增加 Agent 路线占位行（web/api 状态 planned）
- [ ] 3.2 母 change 归档前确认：子 change 均已开单；主规格 `agent` 在首次 runtime/integrate 归档时再 sync（母 change 可先保留 delta，或归档时同步骨架）

## 验证

```bash
openspec validate agent-platform-foundation --strict
```
