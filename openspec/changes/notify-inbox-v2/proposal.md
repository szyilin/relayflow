# 提案：通知中心 V2（notify-inbox-v2 · 母 change · 执行路线图）

## Why

[`org-member-invite-notify`](../archive/2026-07-12-org-member-invite-notify/proposal.md) 已落地站内通知基础设施（`infra_notify`、`NotifyInboxApi`、收件箱 REST、Rail 铃铛），但产品形态仍像「邀请专用信箱」：

- 业务类型仅 `MEMBER_INVITE`；任务 / @我 / 审批等无法聚合
- WebSocket `domain=notify` 仍是 `NoOpNotifyHandler`，`RealtimeEventPublisher` 对 NOTIFY 为 no-op；未读仅靠进入工作台 REST 拉取
- 铃铛列表无类型区分、无点击跳转业务页（deep link）
- 后续 `workspace-tasks-assign`、`bpm-*` 需要统一的投递契约与 UI 承接，否则各模块会各自造通知 UI

飞书路径：铃铛聚合邀请 / 任务 / 审批 / @我，在线实时刷新，点击进入对应上下文。本 change 把通知从「邀请切片」升级为 **可扩展的办公通知中心 V2**，并落地 **至少一个非邀请生产方** 验证端到端。

## What Changes

本 change 为 **母 change（规划真源）**；实现按纵向切片拆子 change，默认 **前端优先**（`-web` → `-api` → `-integrate`）。平台与实时通道可先行。

1. **类型目录（type catalog）**：正式约定并实现 `MEMBER_INVITE` / `TASK_DUE` / `TASK_ASSIGNED` / `IM_MENTION` / `APPROVAL_PENDING` 等常量；payload 统一 deep link 约定（`route` + `entityType` + `entityId`）
2. **幂等键**：`NotifyItemCommand` 支持可选 `dedupeKey`（如 `TASK_DUE:{taskId}`），避免到期提醒刷屏；未读同 key 幂等刷新
3. **收件箱 API 增强**：按 `type` 筛选、全部标已读；`unread-count` 可返回分类型计数（或保持总数 + 列表筛选，见 design）
4. **实时推送必做**：`NotifyInboxApi.push` 成功后经 `RealtimeEventPublisher` 向在线 `user_id` 下发 `{ domain: notify, type: notify.new, payload: { unreadCount, item? } }`
5. **首个非邀请生产方 — 任务到期**：对「我负责的」且 `due_time` 落在提醒窗口的 `TODO` 任务投递 `TASK_DUE`（扫描策略见 design；**不**实现完整指派 / 审批引擎）
6. **前端**：铃铛多类型图标与文案、可选类型筛选、点击跳转、`notify.new` 刷新角标与列表；空状态文案升级
7. **预留**：`TASK_ASSIGNED` / `IM_MENTION` / `APPROVAL_PENDING` 的 push 契约与 UI 分支；生产方分别留给 `workspace-tasks-assign`、`im-mention-*`、`bpm-*`

## Capabilities

### New Capabilities

- （无独立新 domain 目录；通知仍归 `infra` / 工作台 UI）

### Modified Capabilities

- `infra`：通知类型目录、dedupe、API 筛选/全读、WS `domain=notify` 从可选升级为必做
- `task`：到期提醒生产方调用 `NotifyInboxApi`（`TASK_DUE`）
- `im`：仅文档/契约层预留 `IM_MENTION` 投递约定（本 change **不**实现 @ 解析与生产）
- （可选）`web-admin` / 工作台无独立 main spec — UI 行为写入 `infra` scenario 或 lane contract

## Impact

| 层 | 模块/路径 | 变更 |
|----|-----------|------|
| DB | Flyway `V0.1.0.x` | `infra_notify` 增加 `dedupe_key`（可空）+ 索引 |
| 后端 | `relayflow-module-infra-api/biz` | type 常量、Command 字段、Service 幂等、Publisher NOTIFY、Controller 筛选/全读 |
| 后端 | `relayflow-module-task-biz` | 到期扫描/触发 → `NotifyInboxApi.push` |
| 前端 | `web/` notify store、`WorkspaceNotifyBell`、WS 订阅 | 多类型 UI、deep link、实时角标 |
| 规格 | `openspec/specs/infra`、`task` | 归档时同步 |
| 契约 | `openspec/lanes/notify-inbox-v2/contract.md` | 前端 lane 起草 |

**回滚 / 迁移**：新增可空列 `dedupe_key`，兼容旧行；业务类型仅扩展枚举，不破坏 `MEMBER_INVITE`。关闭 task 到期扫描配置即可停生产方。无破坏性 API 删除。

## 非目标

- **审批流引擎**（`bpm-*` / Flowable 等）— 仅预留 `APPROVAL_PENDING` 类型与 payload 形状
- **任务指派他人**（`workspace-tasks-assign`）— 仅预留 `TASK_ASSIGNED`；本 change 不改 assignee 模型
- **IM @ 提及解析与入库** — 仅预留 `IM_MENTION` 类型
- **独立全屏「通知中心」路由页** — V2 仍以 Rail 铃铛 + Modal/抽屉为主；全屏可后续
- **短信 / 邮件 / 推送通道**
- **管理端查看他人通知**
- **全局搜索 `workspace-search-*`**

## 前置

- [`org-member-invite-notify`](../archive/2026-07-12-org-member-invite-notify/proposal.md) 已归档
- [`workspace-tasks-v1`](../archive/2026-07-12-workspace-tasks-v1/proposal.md) 已归档（`task_item.due_time` 可用）
- [`im-realtime-platform`](../archive/2026-07-12-im-realtime-platform/proposal.md) / infra WebSocket 可用

## 子 change 切片（实现顺序）

```text
notify-inbox-v2                         ← 本 change（规划母版）
├── notify-inbox-v2-schema              [平台] dedupe_key + type 常量 + Command
├── notify-inbox-v2-realtime            [平台] push → domain=notify WS fanout
├── notify-inbox-v2-api                 筛选 / 全读 / unread 增强
├── notify-task-due-api                 task-biz 到期生产方
├── notify-inbox-v2-web                 多类型铃铛 UI + deep link + contract
└── notify-inbox-v2-integrate           去 Mock、端到端、看板 done
```

## 后续 change（不在本路线图）

| Change | 说明 |
|--------|------|
| `workspace-tasks-assign` | 指派他人 → `TASK_ASSIGNED` push |
| `workspace-tasks-notify`（可并入 assign） | 更细截止策略、关注人 |
| `im-mention-*` | 消息 @ → `IM_MENTION` |
| `bpm-*` | 审批待办 → `APPROVAL_PENDING` |
| `workspace-search-*` | 全局搜索（独立） |
| `notify-inbox-page` | 独立通知中心页（可选） |
