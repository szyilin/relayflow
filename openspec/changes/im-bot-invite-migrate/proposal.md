## Why

成员邀请触达在拆除 `NotifyInboxApi` 后尚未改道；`invite-helper` 作为独立 Bot 与「组织助手」职责重叠。产品收口：邀请属于组织行为，统一由 **组织助手**（`org-assistant`）投递。

## What Changes

- 邀请成功后调用 `ImBotApi.send`，`botCode=org-assistant`
- 投递范围：被邀请人已有 **ACTIVE** 企业时用 `ALL_ACTIVE_MEMBERSHIPS`（在其当前工作台企业的「组织助手」会话可见）；无 ACTIVE 企业时跳过 Bot 消息（仍依赖注册页 pending invite banner）
- **BREAKING（产品）**：废弃独立 Bot `invite-helper`（软删/停用，不再作为触达入口）
- 幂等：`dedupeKey=MEMBER_INVITE:{invitingTenantId}`
- 文案：`{inviterNickname} 邀请你加入 {tenantName}`；附 deep link 元数据（`entityType=tenant`，`entityId=invitingTenantId`）

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `system`：邀请触达改为 `org-assistant` + `ImBotApi`，不再使用 `invite-helper` / `NotifyInboxApi`
- `im`：种子目录收口——组织类触达（含邀请）归 `org-assistant`；`invite-helper` 退役

## Impact

- `system-biz`：`UserServiceImpl.inviteMember` 恢复触达调用
- Flyway：停用 `invite-helper`，更新 `org-assistant` 描述
- 依赖：已有 `ImBotApi`（`im-bot-notify-foundation`）
- 前端：本切片不改 UI；消息出现在 `/app/messages` 的 `bot_dm`（bot_dm 展示完善见 foundation §6 / 后续 web 切片）
