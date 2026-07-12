# 提案：组织邀请通知（org-member-invite-notify · 母 change · 执行路线图）

## Why

[`multi-tenant-account-v2`](../archive/2026-07-12-multi-tenant-account-v2/proposal.md) 已将「接受邀请」合并进 **注册/登录激活**（`NOT_JOINED → ACTIVE`），但 **被邀请人仍无感知**：

- 管理端 `POST /admin-api/system/user/invite` 仅写库，不触发任何通知
- 注册页不展示「你收到了 N 个企业邀请」
- 工作台无通知入口；`NotifyInboxApi` 在 [`im-platform-foundation`](../archive/2026-07-12-im-platform-foundation/design.md) 中仅为占位

飞书路径：管理员邀请 → 被邀请人收到站内通知/短信 → 注册或登录后自动加入企业。本 change 补齐 **V1 站内通知闭环**（短信通道留 `account-sms-verify`）。

## What Changes

本 change 为 **母 change（规划真源）**；实现按纵向切片拆子 change，默认 **前端优先**（`-web` → `-api` → `-integrate`）。`[平台]` 子 change 可先行。

1. **通知收件箱数据模型**：`infra_notify` 表 + `NotifyInboxApi` 真实实现（替代 V1 no-op）
2. **邀请触发通知**：`UserServiceImpl.inviteMember` 成功后写入 `type=MEMBER_INVITE` 通知（按手机号关联 `user_id`，无账号时仅记 `mobile`）
3. **公开预览 API**：`GET /app-api/system/member-invite/pending?mobile=` — 注册页展示待加入企业列表（permitAll，脱敏）
4. **收件箱 API**：`GET /app-api/infra/notify/page`、`POST .../read` — 工作台查看未读邀请通知
5. **实时推送（可选 V1）**：在线用户经 `domain=notify` WS envelope 刷新未读数（Handler 由占位升级为最小实现）
6. **前端**：注册页邀请横幅；工作台 Rail 通知铃铛 + 列表面板（或 `UModal`）；登录后未读角标

## Capabilities

### New Capabilities

- `notify`：站内通知收件箱（表、API、跨模块 `NotifyInboxApi`）

### Modified Capabilities

- `system`：邀请成功后投递通知；公开 pending 预览 API
- `infra`：`NotifyInboxApi` 实现；`domain=notify` Handler 最小可用
- `web-auth`：注册页展示待接受邀请

## Impact

| 层 | 模块/路径 | 变更 |
|----|-----------|------|
| DB | Flyway `V0.1.0.x` | 新增 `infra_notify` |
| 后端 | `relayflow-module-infra-biz` | NotifyInboxService、app Controller |
| 后端 | `relayflow-module-system-biz` | invite 钩子、pending 预览 |
| 前端 | `web/src/pages/app/register.vue`、`WorkspaceRail` | 邀请横幅、通知 UI |
| 规格 | `openspec/specs/infra`、`system`、`web-auth` | 归档时同步 |

## 非目标

- 短信/邮件真实发送（`account-sms-verify` / 第三方 SMS）
- 邀请链接 token、拒绝邀请、邀请过期
- 完整通知中心（审批、任务、@我 等聚合）— 仅实现 **成员邀请** 类型，接口预留 `type` 枚举扩展
- IM 会话内系统消息代替通知中心（可作为后续增强，非本 change 必做）

## 前置

- [`org-member-invite`](../archive/2026-07-12-org-member-invite/proposal.md) 已归档
- [`multi-tenant-account-v2`](../archive/2026-07-12-multi-tenant-account-v2/proposal.md) §1–§8 已完成（注册激活 `NOT_JOINED`）

## 子 change 切片（实现顺序）

```text
org-member-invite-notify          ← 本 change（规划母版）
├── notify-inbox-schema           [平台] Flyway + NotifyInboxApi + DO
├── org-member-invite-notify-api  邀请钩子 + pending + inbox REST
├── org-member-invite-notify-web  注册横幅 + 工作台通知 UI + contract
└── org-member-invite-notify-integrate  去 Mock、WS 未读（可选）、冒烟
```
