## Context

前置：`org-invite-channels-web`（四渠道 UI + 本地草稿）与 `org-invite-channels-api`（`sys_org_invite` + app-api）均已就绪；`org-invite-settings-integrate` 使管理端设置以 API 为真源。本 change 为 **`-integrate`**：前端接真 API、删客户端草稿真源、看板 done、浏览器走查。

契约真源：[`openspec/lanes/org-invite/contract.md`](../../lanes/org-invite/contract.md) channels 章。

## Goals / Non-Goals

**Goals:**

- `api/app/orgInvite.ts` 调用 capability / shared / shared/reset / mobile / consume
- `orgInvite` store 以 API 为唯一凭证与消费真源；失败可见 `lastError` + toast
- `createHub.canInvite` 以 capability 为准（打开 Modal 前 / Hub 菜单显隐）
- 打开 Modal 时 ensure shared（GET shared）；重置走 POST reset；加入页 consume 成功后 membership 生效
- 看板 `web → done`；`pnpm build` + `pnpm typecheck`；浏览器验收

**Non-Goals:**

- 不改后端表结构 / Controller（缺口另开 `-api` 修补）
- 不改管理端邀请设置页（已 integrate）
- 不重做 Modal / join 页布局（除非字段映射缺口）
- 不做审批列表 UI、Bot 卡推送、admin `user/invite` 委托 UI
- 不接外部联系人 / member-picker

## Decisions

### D1. API 客户端

新建 `web/src/api/app/orgInvite.ts`，用既有 `get` / `post`；路径与类型对齐 contract（camelCase）。

### D2. Store 行为

- `loadCapability()`：`GET …/capability` → 写入 `canInvite` / `inviteMode`；供 createHub 使用
- `ensureShared()`：`GET …/shared` → `token` / `inviteCode` / `expiresAt`；打开 Modal 时调用
- `resetShared()`：`POST …/shared/reset` → 用响应回写；旧凭证由服务端 REVOKED
- `inviteMobile(mobile)`：前端 11 位校验 → `POST …/mobile` → 列表/最近一条用响应
- `consume({ token } | { code })`：`POST …/consume`；成功提示已加入（`alreadyMember` 区分文案）
- 删除 `randomToken` / 本地 `revokedTokens` / `consumeDraft` 草稿校验路径
- 租户切换：`resetForTenantSwitch` 清空缓存字段，下次重新 load

### D3. createHub.canInvite

优先读 `orgInvite` store 的 capability 缓存；进入工作台壳或打开「+」前触发 `loadCapability`（与现有租户切换 reload 挂钩）。禁止继续仅用 `inviteSettings.inviteMode + isAdmin` 作为生产真源（inviteSettings 管理端 store 非工作台成员必 load）。

### D4. 联调与验收

```text
1. docker compose up -d（若需）
2. ./mvnw -pl relayflow-server -am spring-boot:run
3. cd web && pnpm dev
4. /app/login → 工作台 「+」→ 添加企业成员：ensure shared、复制、重置
5. 另一账号或同账号未成员场景：/app/invite/join?t=… 或输入码 → 加入成功
6. curl capability / shared / consume 对照
```

### D5. 看板与文档

- 看板：`org-invite-channels` → Web **done**
- `workspace-ui-patterns`：数据真源改为 API；去掉「草稿 store」表述
- contract：channels 标注 integrate 完成 / 前端已接

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| API 未起或迁移未跑 | tasks 写明先起 server；失败可见错误 |
| capability 未 load 前菜单闪烁 | 默认隐藏或沿用上次缓存；打开「+」前 await load |
| 已是成员 consume 幂等 | UI 展示 `alreadyMember` 友好文案 |
| 无邀请权 403 / FORBIDDEN | 与 `request.ts` 一致；菜单不显示 |

## Migration Plan

1. 部署含 org-invite channels API 的后端并跑 Flyway
2. 发布前端 integrate
3. 回滚：回退前端提交；DB 邀请行保留无害

## Open Questions

- （无）
