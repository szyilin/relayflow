# Tasks：agent-automation-v1

## 0. 深化

- [x] 0.1 本 change **含薄 UI**（不另拆 automation-web）
- [x] 0.2 XXL-JOB：`agentAutomationTick` 每分钟 + 表内 cron/`next_run_at`

## 1. 后端

- [x] 1.1 Automation 表 + App CRUD（+ run-now）
- [x] 1.2 调度执行 + ImBotApi 推送 + seed
- [x] 1.3 compile + 单测

## 2. 前端

- [x] 2.1 `/app/agents` 自动化列表/新建
- [x] 2.2 `pnpm build` + `pnpm typecheck`

### 浏览器验证

1. Flyway 到 `V0.1.0.56`；XXL seed 含 `agentAutomationTick`  
2. `/app/agents` → 自动化 → 新建（需已有 Carrier Bot 的智能体）  
3. 「立即执行」→ `/app/messages` 收到 Bot 推送；Session 历史不变  
