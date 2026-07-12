# 提案：接受组织邀请（org-member-invite-accept）

## Why

[`org-member-invite`](../archive/2026-07-12-org-member-invite/proposal.md) 已实现管理端按手机号邀请，成员状态为 `NOT_JOINED`。被邀请人当前无法登录工作台（`AuthService` 仅允许 `ACTIVE`），邀请流程未闭环。

## What Changes

1. 新增工作台公开 API：预览待接受邀请、接受邀请并设置密码
2. 接受成功后签发 JWT（与现有登录响应一致），成员状态变为 `ACTIVE`
3. 工作台 `/app/invite/accept` 页面：手机号 + 设置密码 + 展示组织名
4. 登录页增加「接受邀请」入口

## Capabilities

### New Capabilities

（无独立 capability；行为归入 `system`）

### Modified Capabilities

- `system`：成员邀请接受与 `NOT_JOINED → ACTIVE` 登录闭环

## Impact

| 层 | 变更 |
|----|------|
| API | `GET/POST /app-api/system/member-invite/*`（permitAll） |
| 前端 | `/app/invite/accept`；`login.vue` 入口链接 |
| DB | 无迁移 |

## 非目标

- 短信/邮件通知与邀请链接 token
- 多租户选择（V1 默认租户）
- 拒绝邀请、邀请过期
