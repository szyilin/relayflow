# Tasks：workspace-agent-web

## 0. 深化

- [x] 0.1 定路由 IA：`/app/agents`；对照 `workspace-ui-patterns.md`
- [x] 0.2 契约已有：`openspec/lanes/workspace-agent/contract.md`（runtime-core 定稿）

## 1. UI

- [x] 1.1 导航入口 + 智能体列表/编辑
- [x] 1.2 新任务 + 历史 + 聊天
- [x] 1.3 store 临时数据（`stores/agent.ts` + `api/app/agent.ts`）
- [x] 1.4 `pnpm build` + `pnpm typecheck`
- [x] 1.5 浏览器验证路径：登录后 → Rail「智能体」或直达 `/app/agents` → 选智能体发新任务 → 历史会话；确认 `/app/messages` 无 Session 混入
