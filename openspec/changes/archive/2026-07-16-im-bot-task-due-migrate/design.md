## Context

- 母 change `im-bot-notify-foundation` 已落地 `ImBotApi`、`task-bot` 种子、system Bot 免订阅；任务到期写路径曾临时断开。
- `TaskDueNotifyService` 已保留窗口判定（`relayflow.task.due-remind-window`，默认 24h）与 `pushIfDueSoon` / `compensateMissingDueReminders` 挂钩点，但 send 为空。
- 主规格 `openspec/specs/task` 已含「Task due bot delivery」基线；本切片补全触发细节并实现代码。
- 触达策略真源：已归档 `im-bot-reach-policy-v1`（system Bot best-effort，失败不挡业务）。

## Goals / Non-Goals

**Goals:**

1. `task-biz` create/update 写路径：窗口内 `TODO` → `ImBotApi.send(task-bot, SINGLE)`
2. `pageMyTasks` 补偿：对窗口内缺提醒的任务补 send（依赖 ImBotApi 幂等）
3. 文案、`dedupeKey`、deep link 与 invite 产方风格一致
4. best-effort：send 异常不回滚任务写操作

**Non-Goals:**

- 全库多租户 cron 扫描
- 任务指派通知（`TASK_ASSIGNED`）
- 可交互卡片 / 群 Bot
- 前端 bot_dm 样式（已有 foundation §6）
- 恢复 `infra_notify` / Rail 铃铛

## Decisions

### D1 — Bot code 与目标

固定 `botCode=task-bot`。目标 `SINGLE`：`tenantId` = 任务租户，`userId` = `assigneeId`。

**备选**：`ALL_ACTIVE_MEMBERSHIPS` → 拒绝；到期提醒只在任务所属企业上下文有意义。

### D2 — 触发策略（沿用 notify-inbox-v2 设计意图）

1. **写路径（主）**：create / update（含 toggle 回到 `TODO` 且仍有 `due_time`）后，若 `shouldRemind` 则 send。
2. **补偿**：`pageMyTasks` 返回前对列表项调用 `compensateMissingDueReminders`；幂等靠 `dedupeKey`。
3. **不做**全库 cron。

窗口：`due_time ∈ [now, now + dueRemindWindow]` 且 `status=TODO`（已有 `shouldRemind`）。

任务标 `DONE`：**不**撤回已发 bot 消息。

### D3 — 幂等与文案

| 项 | 值 |
|----|-----|
| `dedupeKey` | `TASK_DUE:{taskId}` |
| `text` | `「{title}」将在 {dueTime} 到期`（dueTime 可读本地/ISO 均可，实现选简洁格式） |
| `route` | `/app/tasks?taskId={id}` |
| `entityType` | `task` |
| `entityId` | `{taskId}` |

### D4 — 依赖与循环

`task-biz` pom 增加 `relayflow-module-im-api`。若出现与 invite 类似的 Bean 环，对 `ImBotApi` 注入使用 `@Lazy`（与 `UserServiceImpl` 同模式）。

### D5 — Best-effort

```text
try { imBotApi.send(command); }
catch (Exception e) { log.warn(...); }  // 不 throw
```

与邀请产方一致；不新增 `sendBestEffort` API。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 用户未打开 messages 看不到提醒 | 数据落库 + WS `message.new`；产品接受无独立铃铛 |
| 补偿路径重复调用 | `ImBotApi` dedupe |
| send 进事务拖垮 CRUD | catch 吞异常；可选后续改为事务后异步（本切片不做） |

## Migration Plan

1. 合入代码后无 Flyway；已部署环境直接生效。
2. 回滚：还原 `TaskDueNotifyService` no-op + 去掉 im-api 依赖。
3. 离线/内网：无外部推送依赖，仅本机 WS / 拉历史。

## Open Questions

（无）
