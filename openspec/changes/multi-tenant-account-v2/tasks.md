# Tasks：multi-tenant-account-v2（母 change · 执行路线图）

> **用法**：本文件是 V2 多租户账号的 **总路线图**。实际编码按 **子 change** 分批执行；每次会话只做一个子 change 内的一组 task（≤10 条）。  
> **顺序**：默认 **前端优先**（`-web` → `-api` → `-integrate`）；`[平台]` 子 change 可先行。

---

## 0. 规划基线（本 change）

- [x] 0.1 `proposal.md` / `design.md` / spec delta / 本 `tasks.md`
- [x] 0.2 `openspec validate multi-tenant-account-v2 --strict`
- [x] 0.3 团队确认：开发 profile 默认 `tenant.enabled=true`（`application-dev.yml` + `spring.profiles.active=dev`）

### 0.x 开发库数据策略（产品负责人已授权）

> **允许直接清理脏数据，不需要为历史脏数据写适配/回填逻辑。** 见 `design.md` §D11。

- [x] 0.4 **（实施 V2 前执行一次）** 清库重建开发数据库：
- [x] 0.5 启动 `relayflow-server`，确认 Flyway 从头迁移成功（`V0.1.0.1` → 最新）
- [x] 0.6 验证种子：`sys_tenant(id=1)` 存在；按需保留或不再依赖 `admin` 种子（V2 可改为纯注册验证）

**禁止**：为开发期脏数据编写 `UPDATE` 修复脚本、双写兼容、或「旧 invite/accept 数据迁移至新 register 模型」逻辑。

---

## 1. [平台] tenant-bootstrap-api

**目标**：抽取租户初始化逻辑，供「注册建企」与种子安装复用。  
**范围**：Java only；无 `web/`。

- [x] 1.1 新增 `TenantBootstrapService`：创建根部门、owner 主部门、`super_admin` 绑定
- [x] 1.2 从 `V0.1.0.3__seed_admin_user` 逻辑提取为可编程调用（种子脚本仍可用）
- [x] 1.3 单元测试：bootstrap 后 owner 可解析为 `isAdmin`
- [x] 1.4 `./mvnw -pl relayflow-server -am compile`

**验证**：Java 单测 + compile。

**完成后**：可开 `account-register-v2-api`。

---

## 2. account-register-v2-web（前端 lane · 第一步）

**OpenSpec 子 change**：`openspec new change account-register-v2-web`（或会话内声明 lane）

- [x] 2.1 新建 `/app/register`：mobile、password、confirmPassword、nickname、tenantName
- [x] 2.2 `login.vue` 入口改为「没有账号？注册」→ `/app/register`
- [x] 2.3 `/app/invite/accept` 路由 redirect → `/app/register`（保留 mobile query）
- [x] 2.4 新建 `api/app/auth-register.ts` + store 方法（Mock 成功返回 token）
- [x] 2.5 起草 `openspec/lanes/account-register-v2/contract.md`（register API 字段）
- [x] 2.6 `cd web && pnpm build`
- [x] 2.7 浏览器：`/app/login` → 注册页 → Mock 成功进 `/app/messages`

**验证**：`pnpm build` + 浏览器路径。

**完成后**：看板 `web → ui_ready`；可开 `account-register-v2-api`。

---

## 3. account-register-v2-api（后端 lane · 第二步）

**依赖**：`tenant-bootstrap-api` 完成、`account-register-v2-web` contract 就绪

- [x] 3.1 `TenantProperties` 增加 `allowOpenRegister`
- [x] 3.2 `AuthRegisterService` + `POST /app-api/system/auth/register`（permitAll）
- [x] 3.3 实现：建 user + tenant + ACTIVE + bootstrap + 激活所有 NOT_JOINED
- [x] 3.4 错误码：`USER_MOBILE_EXISTS`、`AUTH_REGISTER_PASSWORD_WEAK`
- [x] 3.5 Security 白名单；`enabled=false` 时 register 返回禁用或 404（与 design 一致）
- [x] 3.6 curl 验收（见下方示例）
- [x] 3.7 `./mvnw -pl relayflow-server -am compile`

**curl 示例**：

```bash
curl -s -X POST http://localhost:8080/app-api/system/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"mobile":"13900001234","password":"pass1234","nickname":"张三","tenantName":"张三的工作室"}' | jq
```

**完成后**：archive 子 change `-api`；开 `account-register-v2-integrate`。

---

## 4. account-register-v2-integrate（联调）

- [x] 4.1 store 去 Mock，接真实 `POST /auth/register`
- [x] 4.2 `application-dev.yml` 增加 profile：`tenant.enabled=true`、`allow-open-register=true`
- [x] 4.3 **联调前若数据混乱，再次清库**（§0.4），不做脏数据修复
- [x] 4.4 端到端：注册 → 进工作台 → 管理端可见新 tenant 数据隔离
- [x] 4.5 端到端：管理端邀请 B 手机号 → B 注册 → B 同时拥有自己的企业与 ACTIVE 邀请 tenant
- [x] 4.6 `openspec validate multi-tenant-account-v2 --strict`
- [x] 4.7 `./mvnw -pl relayflow-server -am compile` + `cd web && pnpm build`

---

## 5. tenant-switch-v2-web（前端 lane）

- [x] 5.1 `WorkspaceTenantSwitcher` 组件（header 展示当前企业名）
- [x] 5.2 登录页：处理 `TENANT_SELECTION_REQUIRED` 企业选择 UI
- [x] 5.3 store：`tenants`、`switchTenant()` Mock
- [x] 5.4 起草 `openspec/lanes/tenant-switch-v2/contract.md`（my-list、switch、login 扩展）
- [x] 5.5 `cd web && pnpm build`

---

## 6. tenant-switch-v2-api（后端 lane）

**依赖**：`tenant-switch-v2-web` contract

- [x] 6.1 `GET /app-api/system/tenant/my-list`
- [x] 6.2 `POST /app-api/system/tenant/switch` → 重签 JWT
- [x] 6.3 扩展 `AuthService.login`：mobile 登录、多 tenant、`tenantId` 参数
- [x] 6.4 错误码：`TENANT_SELECTION_REQUIRED`、`AUTH_NO_TENANT`、`TENANT_SWITCH_FORBIDDEN`
- [x] 6.5 `enabled=true` 时 `TenantWebFilter` 从 JWT 读 tenant（核对现有实现）
- [x] 6.6 curl + `./mvnw -pl relayflow-server -am compile`

**curl 示例**：

```bash
# 登录（多企业时应返回 TENANT_SELECTION_REQUIRED）
curl -s -X POST http://localhost:8080/admin-api/system/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"13900001234","password":"pass1234"}' | jq

# 切换
curl -s -X POST http://localhost:8080/app-api/system/tenant/switch \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"tenantId":2}' | jq
```

---

## 7. tenant-switch-v2-integrate（联调）

- [x] 7.1 store 接真实 my-list / switch；切换后 `fetchPermissionInfo` 刷新
- [x] 7.2 WebSocket：切换 tenant 后断开并重连（携带新 token）
- [x] 7.3 端到端：用户 A 拥有 2 企业 → 切换 → 通讯录/消息 tenant 隔离正确
- [x] 7.4 `enabled=false` 回归：现有单租户登录/邀请仍正常（`application.yml` 默认 `enabled=false`，行为未改）
- [x] 7.5 `pnpm build` + 双模式冒烟

---

## 8. member-invite-v2-migrate（收尾）

- [x] 8.1 `UserServiceImpl.inviteMember`：`enabled=true` 时 tenant 来自 JWT，不 fallback default-id
- [x] 8.2 标记 `member-invite/preview|accept` deprecated（Java `@Deprecated` + 文档）
- [x] 8.3 删除或保留后端 endpoint（V2 保留 1 版本兼容，前端不再调用）
- [x] 8.4 更新 `openspec/specs/system` 归档前 diff 检查（见 `specs-sync-checklist.md`）
- [x] 8.5 更新 `AGENTS.md` 下一优先说明

---

## 9. deployment 与文档

- [x] 9.1 `deploy/compose.yml` / README：增加 `RELAYFLOW_TENANT_ENABLED` 说明
- [x] 9.2 `docs/dev/product-permission-model.md` 补充多 tenant 切换语义（若需要）
- [x] 9.3 `docker compose config` 验证

---

## 10. 母 change 归档前

- [ ] 10.1 全部子 change archive
- [ ] 10.2 `openspec validate multi-tenant-account-v2 --strict`
- [ ] 10.3 `openspec archive multi-tenant-account-v2`（同步 specs）
- [ ] 10.4 `./mvnw verify`（如 CI 适用）+ `cd web && pnpm build`

---

## 执行顺序速查

```text
Session 0   §0.4–0.6 清库 + Flyway 重建（实施 V2 前）
Session 1   §1 tenant-bootstrap-api
Session 2   §2 account-register-v2-web
Session 3   §3 account-register-v2-api
Session 4   §4 account-register-v2-integrate
Session 5   §5 tenant-switch-v2-web
Session 6   §6 tenant-switch-v2-api
Session 7   §7 tenant-switch-v2-integrate
Session 8   §8 member-invite-v2-migrate
Session 9   §9 deployment + §10 归档
```

## 后续 change（不在本路线图实现）

| Change | 说明 |
|--------|------|
| `org-member-invite-notify` | 邀请短信/站内信；注册成功后通知 |
| `account-sms-verify` | 手机号验证码 |
| `tenant-install-wizard` | 自托管安装向导配置企业名 |

---

## 会话开场白模板

```text
Using change: account-register-v2-web（multi-tenant-account-v2 子切片 · 前端 lane）
Read: openspec/changes/multi-tenant-account-v2/design.md §D2/D8
Tasks: multi-tenant-account-v2/tasks.md §2
```
