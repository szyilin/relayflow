# 提案：多租户账号与开放注册（multi-tenant-account-v2）

## Why

RelayFlow V1 以 **自托管单企业** 运行（`tenant.enabled=false`、全员落在默认租户 `id=1`），成员加入依赖管理端邀请 + 独立的「接受邀请」页面，与飞书式 **「账号归个人、企业可自建、一账号多组织、手机号激活成员关系」** 产品模型存在差距。

用户已明确选择 **V2 路径**：开放注册、注册时可创建自己的企业、支持多 tenant 成员关系与切换。表结构（`sys_tenant`、`sys_tenant_user`）已在 `tenant-ready-foundation` 预留，现需补齐 **产品能力、API、前端与配置开关**，在不重写业务域的前提下升级到飞书对齐的多租户账号模型。

## What Changes

1. **配置**：新增/启用 `relayflow.tenant.enabled=true` 多租户产品模式；保留 `enabled=false` 自托管兼容路径
2. **开放注册**：工作台公开 API `POST /app-api/system/auth/register`（手机号 + 密码 + 昵称 + 企业名称）
3. **注册即建企**：注册成功创建 `sys_user` + 新 `sys_tenant` + `sys_tenant_user(ACTIVE)`，创建人设为 `owner_user_id` 并绑定 `super_admin`
4. **邀请激活合并进注册**：若该手机号存在其他租户的 `NOT_JOINED` 关系，注册/设密后 **一并激活**（飞书：同一手机号登录即激活待加入企业）
5. **多 tenant 登录与切换**：登录后可列出所属企业；切换当前企业重新签发含目标 `tenant_id` 的 JWT
6. **登录增强**：支持 **手机号或用户名** 登录；JWT `tenant_id` 来自当前选中企业（`enabled=true` 时）
7. **前端**：`/app/register` 注册页；登录页入口改为「没有账号？注册」；工作台壳层 **企业切换器**
8. **废弃独立邀请接受页语义**：`/app/invite/accept` 合并为注册流程（路由可重定向）；`member-invite/accept` API 标记废弃并由 `auth/register` 承接
9. **管理端邀请**：语义不变，但邀请目标 tenant 为 **当前管理端上下文 tenant**（`enabled=true` 时从 JWT 解析，非硬编码 `default-id`）
10. **BREAKING（enabled=true 部署）**：新注册用户不再自动进入 `tenant_id=1`；现有种子租户 `id=1` 保留但与新注册企业隔离

## Capabilities

### New Capabilities

（无独立新 capability；行为归入已有 `system`、`web-auth`、`deployment`）

### Modified Capabilities

- `system`：开放注册、注册建企、多 tenant 登录/切换、邀请激活并入注册、JWT 租户上下文
- `web-auth`：注册页、企业切换 UI、登录/注册入口文案与路由
- `deployment`：`tenant.enabled` 多租户模式配置说明与 Compose 环境变量

## Impact

| 层 | 模块/路径 | 变更 |
|----|-----------|------|
| 配置 | `relayflow-server/application*.yml`、`TenantProperties` | `enabled`、`allow-open-register` |
| 框架 | `relayflow-spring-boot-starter-tenant`、`starter-security` | JWT 解析 tenant；Filter 行为 |
| 后端 | `relayflow-module-system-biz` | `AuthService`、`TenantService`、注册/切换 Controller |
| DB | Flyway `V0.1.0.x` | 无破坏性表结构变更；可选索引；种子策略文档化；**开发期允许清库重建，不做脏数据适配**（见 design D11） |
| 前端 | `web/src/pages/app/`、`stores/auth.ts`、`components/workspace/` | register、tenant switcher |
| 规格 | `openspec/specs/system|web-auth|deployment` | 归档本 change 时同步 |
| 自托管 | `enabled=false` | **无行为变化**；回归须通过 |

## 非目标（本 change）

- 短信/邮件验证码（V2 仍可用手机号 + 密码；验证码留后续 `account-sms-verify`）
- 个人号（飞书个人帐号）— 仅「创建企业」路径
- 租户计费、平台超管控制台、子域名解析租户（V3）
- 拒绝邀请、邀请过期、邀请链接 token
- Gateway/Nacos/微服务拆分（Phase 2）

## 执行策略

本 change 为 **母 change（规划真源）**；实现按纵向切片拆为子 change（见 `tasks.md`），默认 **前端优先**（`-web` → `-api` → `-integrate`）。单次会话仅执行一个子 change 内 ≤10 条 task。
