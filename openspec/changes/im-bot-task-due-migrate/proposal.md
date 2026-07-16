## Why

`im-bot-notify-foundation` 已拆除 `NotifyInboxApi` / `infra_notify`，任务到期提醒在 `TaskDueNotifyService` 中仍为 no-op。主规格已要求走 `task-bot` + `ImBotApi`，本切片把产方接上，恢复「即将到期」可触达。

## What Changes

- `task-biz`：在 create/update 写路径与列表补偿路径调用 `ImBotApi.send`，`botCode=task-bot`，目标 `SINGLE`
- 幂等：`dedupeKey=TASK_DUE:{taskId}`
- 文案与 deep link：到期提醒文案 + `route=/app/tasks?taskId=`、`entityType=task`
- 触达 **best-effort**：`ImBotApi.send` 失败只记日志，不阻挡任务 CRUD
- **不**恢复 `NotifyInboxApi` / Rail 铃铛 / `domain=notify` 业务信封
- 前端：本切片不改 UI；消息出现在 `/app/messages` 的 `task-bot` bot_dm

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `task`：补全到期提醒行为——提醒窗口、写路径触发、列表补偿、文案/deep link、best-effort；明确禁止再走 notify 收件箱

## Impact

- `relayflow-module-task-biz`：实现 `TaskDueNotifyService`；pom 增加 `relayflow-module-im-api`
- 依赖：已有 `ImBotApi`、`task-bot` 种子（foundation）；触达策略见已归档 `im-bot-reach-policy-v1`（system Bot 免订阅）
- 回滚：去掉 send 调用即可；无 Flyway；已落库的 bot_dm 消息保留
- 涉及：Java（task-biz）；**不**改 `web/` / `deploy/`
