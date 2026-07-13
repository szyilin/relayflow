# 设计：通知中心 V2（notify-inbox-v2）

## Context

- **已有**：`infra_notify`、`NotifyInboxApi` / `NotifyInboxServiceImpl`、`AppNotifyController`（page / unread-count / read）、`WorkspaceNotifyBell` + `stores/notify`、`MEMBER_INVITE` 生产方（`system-biz`）
- **缺口**：类型仅邀请；`RealtimeEventPublisherImpl` 对 `NOTIFY` no-op；`NoOpNotifyHandler`；无 deep link；无 dedupe 实体键；task 有 `due_time` 但不投递通知
- **约束**：跨域仅 `*-api`；`task-biz` / `system-biz` 只调 `NotifyInboxApi`，禁止碰 `infra_notify` Mapper；自部署、无外部推送服务

## Goals / Non-Goals

**Goals:**

- 通知中心成为多类型聚合入口（UI + 类型目录 + 幂等）
- 在线用户实时收到未读刷新（REST 仍为权威与离线兜底）
- 任务到期成为首个非邀请生产方，证明扩展路径
- 为指派 / @我 / 审批预留 type 与 payload 形状

**Non-Goals:**

- bpm 引擎、任务指派、IM @ 解析、独立通知页、短信邮件

## Decisions

### D1：类型目录（`InfraNotifyType`）

| type | 含义 | 本 change 生产方 | payload 最低字段 |
|------|------|------------------|------------------|
| `MEMBER_INVITE` | 企业邀请 | 已有 system | `tenantName`, `inviterNickname` |
| `TASK_DUE` | 任务到期/将到期 | **本 change task-biz** | `entityType=task`, `entityId`, `route=/app/tasks?taskId=` |
| `TASK_ASSIGNED` | 任务指派 | 预留（assign change） | 同上 + `assignerNickname` |
| `IM_MENTION` | 消息 @我 | 预留 | `entityType=im_message`, `conversationId`, `route=/app/messages?...` |
| `APPROVAL_PENDING` | 审批待办 | 预留（bpm） | `entityType=approval`, `route` TBD |

- 未知 type：**允许入库与展示**（前端回退默认图标 + 标题/正文），不因未知 type 拒绝 push
- 常量集中在 `infra-api`：`InfraNotifyType`；业务模块引用常量字符串，避免拼写漂移

**备选**：每类型独立表 → 拒绝，增加聚合复杂度。

### D2：幂等键 `dedupe_key`

Flyway（序号取当前最大 +1，实现时确认）：

```sql
ALTER TABLE infra_notify
    ADD COLUMN IF NOT EXISTS dedupe_key VARCHAR(128);

CREATE INDEX IF NOT EXISTS idx_infra_notify_dedupe
    ON infra_notify (tenant_id, user_id, type, dedupe_key, read_flag)
    WHERE deleted = 0 AND dedupe_key IS NOT NULL;
```

`NotifyItemCommand` 增加可选 `dedupeKey`：

| 场景 | dedupeKey 示例 | 行为 |
|------|----------------|------|
| 邀请 | 可空；沿用现逻辑（tenant+user/mobile+type 未读幂等） | 兼容 V1 |
| 任务到期 | `task:{taskId}` | 未读同 key → 更新 title/body/payload/time，不新增行 |
| 指派 | `task-assign:{taskId}:{assigneeId}` | 预留 |

无 `dedupeKey` 时：保持 V1 `findUnreadDuplicate`（tenant + receiver + type + unread）。

**备选**：把 entityId 塞进 type → 拒绝，污染枚举。

### D3：实时推送（必做）

```text
NotifyInboxServiceImpl.push 成功
  → 若 userId != null
      RealtimeEventPublisher.publish(
        domain=notify,
        type=notify.new,
        tenantId,
        targetUserIds=[userId],
        payload={ unreadCount, id?, type?, title? }
      )
```

- 升级 `RealtimeEventPublisherImpl`：`NOTIFY` 走与 `SYSTEM` 相同的 `sendToUsers` 路径（或抽公共 `pushIfTargets`）
- 上行 `domain=notify` 客户端消息：仍可忽略（保留 Handler，改为 debug 日志或空实现即可，**不**要求客户端上行）
- `userId == null`（仅 mobile 的邀请）：不发 WS；注册回填后下次 push 或登录 REST 可见

**前端**：复用现有工作台 WebSocket 连接（`useImWebSocket` 或抽 `useRealtimeSocket`），监听 `domain=== 'notify' && type=== 'notify.new'` → `notifyStore.fetchUnreadCount()` / 可选 prepend。

**备选**：轮询 30s → 拒绝作为主路径，仅作弱网兜底（进入页面仍拉 REST）。

### D4：收件箱 API 增强

| 端点 | 变更 |
|------|------|
| `GET /app-api/infra/notify/page` | 可选 query `type`；返回项增加 `dedupeKey`（可选，前端可不展示） |
| `GET /app-api/infra/notify/unread-count` | 保持 `{ unreadCount }`；**可选** `byType: { MEMBER_INVITE: n, TASK_DUE: m }`（V2 实现；前端角标仍用总数） |
| `POST /app-api/infra/notify/read` | 现有 `{ ids }` 不变 |
| `POST /app-api/infra/notify/read-all` | **新增** body 可空或 `{ type? }`：当前用户全部/按类型标已读 |

鉴权：JWT + 有效成员；仅操作 `user_id = 当前用户` 的行。

### D5：任务到期生产方

**触发策略（V2 选「写路径 + 轻量扫描」组合，优先写路径）：**

1. **写路径（主）**：`TaskItemServiceImpl` 在 create/update 当 `due_time != null` 且 `status=TODO` 且 `due_time` 落在 **[now, now+24h]**（可配置 `relayflow.task.due-remind-window`，默认 24h）时，对 `assignee_id` push `TASK_DUE`。
2. **扫描**：V2 **不做全库 cron**（多租户上下文复杂）。补偿路径为 **必做 lazy check**：`pageMyTasks` 时对即将到期且尚未有未读 `TASK_DUE` 的任务补 push。

**文案示例**：

- title: `任务即将到期`
- body: `「{title}」将在 {dueTime} 到期`
- route: `/app/tasks?taskId={id}`

任务标为 `DONE`：**不**自动删除通知；用户可标已读。后续可增强「完成即已读」。

`task-biz` 已依赖或可依赖 `infra-api`（`NotifyInboxApi`）——实现时确认 pom；禁止依赖 `infra-biz`。

### D6：payload / deep link 约定

```json
{
  "route": "/app/tasks?taskId=123",
  "entityType": "task",
  "entityId": "123",
  "dueTime": "2026-07-13T18:00:00+08:00"
}
```

- 前端点击：若 `payload.route` 为站内相对路径则以 `router.push`；非法或外链忽略，仅标已读
- `MEMBER_INVITE`：无 route 时保持现有说明文案（企业切换器）

### D7：前端 UI

```text
WorkspaceNotifyBell Modal
  ├── 头部：通知 | [全部标已读]
  ├── 可选 Chip：全部 | 邀请 | 任务 | 其他
  └── 列表项：
        图标(type) | title / body | time
        点击 → 标已读 + route push
```

| type | 图标建议 |
|------|----------|
| MEMBER_INVITE | `i-lucide-building-2` |
| TASK_DUE / TASK_ASSIGNED | `i-lucide-check-square` |
| IM_MENTION | `i-lucide-at-sign` |
| APPROVAL_PENDING | `i-lucide-file-check` |
| default | `i-lucide-bell` |

空状态：`企业邀请、任务提醒等会显示在这里`。

### D8：与后续生产方的契约

```text
任意 *-biz
  → NotifyInboxApi.push(NotifyItemCommand{
       tenantId, userId, type, title, body,
       dedupeKey?, payload: { route, entityType, entityId, ... }
     })
  → 不得自行发 WS；由 infra push 内 fanout
```

文档同步：`docs/dev/api-integration-board.md` 登记切片；lane `openspec/lanes/notify-inbox-v2/contract.md`。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 写路径漏扫导致「创建后很久才到期」无提醒 | lazy check on list；后续可加带 TenantContext 的定时任务 |
| WS 丢失时角标陈旧 | 进入工作台 / 打开铃铛仍 REST 拉取 |
| 未知 type 前端难看 | 默认图标 + 仅展示 title/body |
| dedupe 索引选择性 | 仅 `dedupe_key IS NOT NULL` 部分索引 |
| task 模块循环依赖 | 只依赖 `infra-api` |

## Migration Plan

1. Flyway 加 `dedupe_key`（可空）→ 旧数据不受影响  
2. 先发后端 API + WS，前端仍兼容旧字段  
3. 再发前端多类型 UI + WS 监听  
4. 启用 task 到期 push  
5. 回滚：降级前端；关 task 配置；列可保留

## Open Questions

1. **到期窗口**默认 24h 是否合适？→ 实现用配置，默认 24h。  
2. **unread byType** 是否必须？→ API 可选实现；前端 V2 可用列表 filter，角标用总数。  
3. **独立 `/app/notify` 页**是否提前？→ 本 change 不做，Open Questions 关闭为非目标。

## 验证

```bash
openspec validate notify-inbox-v2 --strict
./mvnw -pl relayflow-server -am compile
cd web && pnpm build
# 浏览器：
# 1) 管理端邀请 → 在线被邀请用户铃铛实时角标（已有账号）
# 2) /app/tasks 创建 1h 内到期任务 → 铃铛出现 TASK_DUE → 点击进入任务
```

## 看板登记

| 切片 | 页面 | 端点 |
|------|------|------|
| notify-inbox-v2 | Rail 铃铛 | `infra/notify/*`、`domain=notify` WS |
| notify-task-due | `/app/tasks` | 内部 `NotifyInboxApi` |
