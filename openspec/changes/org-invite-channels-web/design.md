## Context

设置页与 `inviteSettings` store 已就绪；`WorkspaceOrgInviteModal` 四 Tab 为占位；`createHub.canInvite` 已派生自 `inviteMode`。

## Goals / Non-Goals

**Goals:**

- 四渠道可演示（本地 token/码/手机号草稿）
- 加入页可打开并校验本地草稿
- contract 补 channels 章；看板 ui_ready

**Non-Goals:**

- 真 API、DB、Bot、注册激活迁移、审批列表真后端

## Decisions

1. Store `orgInviteDraft`（或扩 `inviteSettings`/`createHub`）：共享 `token`、`inviteCode`、`expiresAt`、`mobileInvites[]`；重置换新 token/码。
2. 失效时间：管理员渠道固定展示「不过期（草稿）」或 30 天；非管理员按 `nonAdminExpireDays`（即便当前用户是 admin 创建共享渠道，V1 web 用 settings 天数统一展示）。
3. 加入路由：`/app/invite/join?t=` 与 `?code=`；未登录 → login redirect；已登录则尝试消费草稿。
4. 手机号：仅校验 11 位并写入草稿列表；toast 说明待 api。

## Risks / Trade-offs

- [Risk] 草稿与真 API 字段漂移 → contract 先定字段名
- [Risk] 加入页与旧 `/app/invite/accept` 混淆 → 新路径 `join`；accept 仍 redirect register

## Open Questions

- 无阻塞
