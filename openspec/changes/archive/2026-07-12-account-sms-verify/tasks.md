# Tasks：account-sms-verify

> **顺序**：默认 **前端优先**（`-web` → `-api` → `-integrate`）。后端 `[平台]` 段可单独会话先行。

---

## 0. 规划基线

- [x] 0.1 `proposal.md` / `design.md` / spec delta / 本 `tasks.md`
- [x] 0.2 `openspec validate account-sms-verify --strict`

---

## 1. [平台] account-sms-verify-api

**目标**：`SmsCodeService` + 发送 API + 注册校验。  
**范围**：Java + 配置；无 `web/`。

- [ ] 1.1 `SmsProperties`（enabled、mock、ttl、resend-interval、daily-limit）
- [ ] 1.2 `SmsSender` SPI + `MockSmsSender`（`mock=true`）
- [ ] 1.3 `SmsCodeService`：Redis 存码、限流、校验
- [ ] 1.4 `POST /app-api/system/auth/sms/send`（permitAll；scene=register）
- [ ] 1.5 `AuthRegisterServiceImpl`：`enabled=true` 时校验 `smsCode`
- [ ] 1.6 错误码：`SMS_*` 系列；`application-dev.yml` 示例配置
- [ ] 1.7 单测 + curl + `./mvnw -pl relayflow-server -am compile`

**验证**：单测 + compile + curl 发送后 Redis 有 key。

**完成后**：可开 `-web`。

---

## 2. account-sms-verify-web（前端 lane）

- [ ] 2.1 起草 `openspec/lanes/account-sms-verify/contract.md`
- [ ] 2.2 `api/app/auth-sms.ts`；`stores/auth.ts` 增加 `sendRegisterSms`
- [ ] 2.3 `/app/register`：验证码输入 + 获取验证码倒计时
- [ ] 2.4 `enabled=false` 时隐藏验证码 UI（环境变量或 API 探测二选一，contract 约定）
- [ ] 2.5 `cd web && pnpm build`
- [ ] 2.6 浏览器：Mock 模式注册页布局正确

**验证**：`pnpm build`。

**完成后**：看板 web → `ui_ready`（若看板已登记）。

---

## 3. account-sms-verify-integrate（联调）

- [ ] 3.1 注册 payload 带 `smsCode`；去 Mock
- [ ] 3.2 dev profile：`sms.enabled=true, mock=true` → 日志取码 → 注册成功
- [ ] 3.3 `sms.enabled=false` 回归：注册无需验证码
- [ ] 3.4 `openspec validate account-sms-verify --strict`
- [ ] 3.5 `./mvnw -pl relayflow-server -am compile` + `cd web && pnpm build`

---

## 4. 归档

- [x] 4.1 `openspec archive account-sms-verify`（同步 `system`、`web-auth` specs）
- [x] 4.2 更新 `AGENTS.md` 下一优先（若本 change 完成后）

> **说明**：本 change 仅归档规划规格，**实现暂缓**（前期不开发，避免拖慢迭代）。

---

## 执行顺序速查

```text
Session 1   §1 account-sms-verify-api
Session 2   §2 account-sms-verify-web
Session 3   §3 integrate + §4 归档
```

## 会话开场白模板

```text
Using change: account-sms-verify（后端 lane · §1）
Read: openspec/changes/account-sms-verify/design.md §D2–D4
Tasks: account-sms-verify/tasks.md §1
```
