## Context

aily 自动化：定时跑 Agent → Carrier Bot 推送到 IM。依赖 `im-bot-agent-carrier-v1`。

## Goals / Non-Goals

**Goals**

- `agent_automation` 表 + App CRUD
- XXL-JOB 分钟 tick：到期任务 → AgentRuntime → `ImBotApi.send`（carrier bot）
- 结果**不写** `agent_session`
- 本 change **含薄 UI**（`/app/agents` 内自动化列表/新建），不另拆 `-web`

**Non-Goals**

- 群推送、闲时队列、NL 推送条件、多智能体
- 动态在 XXL Admin 注册每条 Cron（用应用内 cron + next_run_at）

## Decisions

### D1 — 调度：XXL tick + 表内 cron

`@XxlJob("agentAutomationTick")` 每分钟跑；表存 `cron_expr` + `next_run_at`；用 Spring `CronExpression` 推进下次时间。种子 Job 写入 `deploy/xxl-job/seed`。

### D2 — 结果只进 IM

`ImBotApi.send`，botCode=`agent-carrier-{agentId}`；不写 Session。

### D3 — 推送目标 V1

仅 `target_user_id`（默认所有者）bot_dm；群目标后置。

### D4 — Job 身份

Tick 内 `TenantContextHolder` + 临时 `LoginUser(owner)` SecurityContext，使 Skill 以所有者身份执行。

### D5 — UI 同 change

`/app/agents` 增加「自动化」面板；不新开 Rail。

## Risks / Trade-offs

[Skill 依赖登录态] → Job 注入 LoginUser  
[LLM 慢阻塞 tick] → 单次串行、失败记 last_error；后续可异步

## Open Questions

- 无
