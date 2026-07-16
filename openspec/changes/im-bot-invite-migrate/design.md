## Context

- 母 change `im-bot-notify-foundation` 已落地 `ImBotApi`、bot_dm、enablement；邀请写路径曾临时断开。
- 产品拍板：不保留独立「邀请助手」；邀请归 **组织助手**（`org-assistant`）。

## Goals / Non-Goals

**Goals:**

1. `inviteMember` 成功 → `ImBotApi.send(org-assistant)`
2. 停用种子 Bot `invite-helper`
3. 明确无 ACTIVE 企业时的降级（仅 banner）

**Non-Goals:**

- bot_dm 前端样式完善（foundation §6）
- 任务/审批/账号安全 Bot 调整
- 新建「管理员小助手」Bot

## Decisions

### D1 — Bot code

固定 `org-assistant`（显示名「组织助手」）。不新增 code。

### D2 — Target scope

| 被邀请人状态 | 行为 |
|--------------|------|
| ≥1 个 ACTIVE 企业 | `ALL_ACTIVE_MEMBERSHIPS`：在其每个 ACTIVE 企业的组织助手 bot_dm 各写一条 |
| 无 ACTIVE（仅新号 / 仅 NOT_JOINED） | **不调用** `ImBotApi`；邀请仍成功；靠注册页 pending banner |

邀请企业本身是 `NOT_JOINED`，该企业下尚无 user enablement；故不以 SINGLE 打邀请企业。

### D3 — 幂等与文案

- `dedupeKey = MEMBER_INVITE:{invitingTenantId}`
- text：`{inviterNickname} 邀请你加入 {tenantName}`
- `route` 可空或预留工作台切换相关路径；`entityType=tenant`，`entityId={invitingTenantId}`

### D4 — invite-helper 退役

新 Flyway：`im_bot` 中 `invite-helper` 置 `deleted=1`（或 `status=0`），清理其 tenant/user enablement；更新 `org-assistant.description` 标明含成员邀请。

## Risks

| 风险 | 缓解 |
|------|------|
| 用户看不到 bot_dm UI | 数据仍落库；后续 §6；联调可查 DB / 会话 API |
| send 失败回滚邀请 | 与旧 notify 同事务语义；保持 |
