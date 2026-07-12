# Tasks：org-member-invite-notify（母 change · 执行路线图）

> **用法**：本文件是邀请通知的 **总路线图**。实际编码按 **子 change** 分批执行；每次会话只做一个子 change 内的一组 task（≤10 条）。  
> **顺序**：默认 **前端优先**（`-web` → `-api` → `-integrate`）；`[平台]` 子 change 可先行。

---

## 0. 规划基线（本 change）

- [x] 0.1 `proposal.md` / `design.md` / spec delta / 本 `tasks.md`
- [x] 0.2 `openspec validate org-member-invite-notify --strict`
- [x] 0.3 更新 `docs/dev/api-integration-board.md` 登记 `org-member-invite-notify` 切片（planned）

---

## 1. [平台] notify-inbox-schema

**目标**：`infra_notify` 表 + `NotifyInboxApi` 实现 + codegen DO。  
**范围**：Java only；无 `web/`。

- [x] 1.1 Flyway `V0.1.0.{n}__infra_notify.sql`
- [x] 1.2 `NotifyInboxApi` + `NotifyInboxServiceImpl`（`infra-api` / `infra-biz`）
- [x] 1.3 `./scripts/codegen.sh --module infra --tables infra_notify` → diff 合并 DO/Mapper
- [x] 1.4 单元测试：push、按 user/mobile 查询、幂等更新
- [x] 1.5 `./mvnw -pl relayflow-server -am compile`

**验证**：单测 + compile。

**完成后**：可开 `org-member-invite-notify-api`。

---

## 2. org-member-invite-notify-api（后端 lane）

**依赖**：`notify-inbox-schema` 完成

- [x] 2.1 `UserServiceImpl.inviteMember` 成功后调用 `NotifyInboxApi.push(MEMBER_INVITE)`
- [x] 2.2 `GET /app-api/system/member-invite/pending?mobile=`（permitAll）
- [x] 2.3 `GET /app-api/infra/notify/page`、`unread-count`、`POST .../read`
- [x] 2.4 `AuthRegisterServiceImpl` 注册成功后回填 `infra_notify.user_id`
- [x] 2.5 Security 白名单 pending；notify 端点 JWT
- [x] 2.6 curl 验收（见 `design.md` D4/D5）
- [x] 2.7 `./mvnw -pl relayflow-server -am compile`

**完成后**：看板 api → `ready`；可开 `-web`。

---

## 3. org-member-invite-notify-web（前端 lane）

- [x] 3.1 起草 `openspec/lanes/org-member-invite-notify/contract.md`
- [x] 3.2 `api/app/member-invite-pending.ts`、`api/app/notify.ts`、`stores/notify.ts`
- [x] 3.3 `/app/register`：mobile debounce → pending 横幅 `UAlert`
- [x] 3.4 `WorkspaceRailHeader`：`WorkspaceNotifyBell` 未读角标 + `UModal` 列表
- [x] 3.5 `cd web && pnpm build`
- [x] 3.6 浏览器：`/app/register` 输入已邀请手机号 → 见横幅（接真实 API）

**验证**：`pnpm build` + 浏览器路径。

**完成后**：看板 web → `ui_ready`。

---

## 4. org-member-invite-notify-integrate（联调）

- [x] 4.1 store 去 Mock，接真实 pending + notify API
- [x] 4.2 端到端：管理端邀请 → 注册页横幅 → 注册 → 铃铛有历史通知（见 contract 浏览器路径）
- [ ] 4.3 （可选）`domain=notify` WS 未读刷新
- [x] 4.4 `openspec validate org-member-invite-notify --strict`
- [x] 4.5 `./mvnw -pl relayflow-server -am compile` + `cd web && pnpm build`
- [x] 4.6 看板 `org-member-invite-notify` → **done**

---

## 5. 母 change 归档前

- [x] 5.1 全部子 change archive
- [x] 5.2 `openspec archive org-member-invite-notify`（同步 specs）
- [x] 5.3 `./mvnw verify`（如适用）+ `cd web && pnpm build`

---

## 执行顺序速查

```text
Session 1   §1 notify-inbox-schema
Session 2   §2 org-member-invite-notify-api
Session 3   §3 org-member-invite-notify-web
Session 4   §4 integrate + §5 归档
```

## 后续 change（不在本路线图）

| Change | 说明 |
|--------|------|
| `account-sms-verify` | 邀请短信通道 |
| `notify-inbox-v2` | 审批/任务/@我 聚合 |

---

## 会话开场白模板

```text
Using change: org-member-invite-notify-web（org-member-invite-notify 子切片 · 前端 lane）
Read: openspec/changes/org-member-invite-notify/design.md §D6/D7
Tasks: org-member-invite-notify/tasks.md §3
```
