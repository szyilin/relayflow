## Why

对齐 aily「自动化」：定时/触发跑 Agent，结果经 Carrier Bot **推送到**用户或群（`/app/messages`），不进入智能体 Session 历史（除非产品另开运行日志）。

## What Changes

- Automation 实体：标题、描述/任务提示、指派 Agent、触发器（先 cron）、推送目标
- 调度：XXL-JOB 或现有调度约定（深化拍板）
- 执行：AgentRuntime.run → `ImBotApi.send`（经 carrier bot）
- 可选：自然语言「推送条件」（后置，先做开关/简单规则）

## Capabilities

### Modified Capabilities

- `agent`：Automation 定义与执行
- `im`：仅复用 send，不新写真源

## Impact

- `agent-biz` automation；依赖 `im-bot-agent-carrier-v1`
- 前端自动化列表/新建（可另拆 `-web`——深化时决定是否本 change 含 UI）

## 非目标

- 多智能体小队、闲时队列优化（aily 闲时执行可后置）
