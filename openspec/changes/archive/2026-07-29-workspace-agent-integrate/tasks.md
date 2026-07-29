# Tasks：workspace-agent-integrate

## 0. 深化

- [x] 0.1 确认 runtime-core 与 web contract 一致（`openspec/lanes/workspace-agent/contract.md`）

## 1. 联调

- [x] 1.1 去掉 Agent 相关 store Mock
- [x] 1.2 联调 CRUD + 对话（store 接真 API；页面加载/错误态）
- [x] 1.3 `pnpm build` + `pnpm typecheck` + 浏览器路径
- [x] 1.4 更新 api-integration-board

### 浏览器验证路径

1. 后端已启动；管理端 `/admin/infra/llm` 至少启用一条模型连接  
2. 登录工作台 → `/app/agents`  
3. 新建智能体（选 `modelRef`）→ 新任务发消息 → 收到 Runtime 回复  
4. 历史任务可重开；确认 `/app/messages` 无 Session 混入  
