# 提案：手机号短信验证码（account-sms-verify）

> **归档说明（2026-07-12）**：本 change **仅归档规划规格**，delta 已手动合并至 `openspec/specs/system`、`web-auth`。**V1 前期暂缓实现**，待开放注册 spam 风险上升或产品要求时再立项 `-api` / `-web`。

## Why

[`multi-tenant-account-v2`](../archive/2026-07-12-multi-tenant-account-v2/proposal.md) 开放注册使用 **手机号 + 密码**，无验证码环节。设计文档已标注 spam 风险，缓解手段为 `allow-open-register` 开关 + **后续验证码**。

飞书注册/登录普遍有短信验证码。本 change 在 **可配置启用** 的前提下，为注册（必选场景）与找回密码（可选 V1 占位）提供验证码能力；开发环境使用 **Mock 发送器**（日志输出），生产可接阿里云/腾讯云等（接口抽象，具体厂商非本 change 必做）。

## What Changes

1. **配置**：`relayflow.sms.enabled`、`relayflow.sms.mock=true`（dev 默认 mock）
2. **发送 API**：`POST /app-api/system/auth/sms/send` — `scene=register|reset_password`，限流 + Redis TTL 存储 6 位码
3. **校验**：注册 `POST /auth/register` 增加可选 `smsCode`；`enabled=true` 时必填
4. **前端**：`/app/register` 增加验证码输入 + 「获取验证码」倒计时按钮
5. **错误码**：`SMS_SEND_TOO_FREQUENT`、`SMS_CODE_INVALID`、`SMS_CODE_EXPIRED`

## Capabilities

### New Capabilities

（无独立 capability；行为归入 `system`）

### Modified Capabilities

- `system`：短信验证码发送与校验；注册请求字段扩展
- `web-auth`：注册页验证码 UI

## Impact

| 层 | 模块/路径 | 变更 |
|----|-----------|------|
| 配置 | `SmsProperties` | enabled、mock、ttl、rate limit |
| 后端 | `relayflow-module-system-biz` | `SmsCodeService`、Redis |
| 前端 | `web/src/pages/app/register.vue` | 验证码表单项 |
| Redis | 键 `sms:code:{scene}:{mobile}` | TTL 5min |
| 规格 | `openspec/specs/system`、`web-auth` | 归档时同步 |

## 非目标

- 真实 SMS 厂商对接实现（仅 `SmsSender` SPI + `MockSmsSender` + 文档说明接入方式）
- 登录二次验证（2FA）
- 图形验证码
- 邀请通知短信（`org-member-invite-notify` 站内信优先）

## 前置

- `multi-tenant-account-v2` 注册 API 已存在
- Redis 已部署（Compose 已有）

## 执行策略

单 change 内按 lane 拆分 task 章节（`-web` → `-api` → `-integrate`），不另开母 change；可与 `org-member-invite-notify`、`workspace-tasks-v1` **并行**，但建议在开放注册对外暴露前完成。
