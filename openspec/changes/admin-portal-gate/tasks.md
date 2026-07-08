# 任务：admin-portal-gate

> **范围**：管理后台门户准入 + `isAdmin` + 登录门禁强化。约定见 [`product-permission-model.md`](../../docs/dev/product-permission-model.md)。

## 前置

- [ ] 0.1 阅读本 change 的 `proposal.md`、`design.md`、spec delta
- [ ] 0.2 阅读 [`openspec/lanes/system-rbac-kernel/contract.md`](../../lanes/system-rbac-kernel/contract.md)

## 1. 契约

- [ ] 1.1 更新 `openspec/lanes/system-rbac-kernel/contract.md`：`get-permission-info` 增加 `isAdmin` 字段与 curl 示例

## 2. 后端

- [ ] 2.1 `PermissionService`：提供 `isAdmin(userId, tenantId)`（或与 permission codes 同源）
- [ ] 2.2 `AuthPermissionInfoRespVO` + `get-permission-info` 返回 `isAdmin`
- [ ] 2.3 `AdminPortalAuthorizationFilter`（或等价）：`/admin-api/**` 零 authorities → 403；白名单见 design D4
- [ ] 2.4 `./mvnw -pl relayflow-server -am compile`
- [ ] 2.5 curl：`admin` → `isAdmin=true`；构造零权限用户 → `isAdmin=false` 且 `/admin-api/system/user/page` → 403

## 3. 前端（web/）

- [ ] 3.1 `api/admin/auth.ts`、`stores/auth.ts`：类型与 `isAdmin` 状态
- [ ] 3.2 `router/guards.ts`：已登录非 admin 访问 `/admin/**` → `/app/no-admin-access`；确认未登录 redirect login
- [ ] 3.3 新建 `pages/app/no-admin-access.vue`（workspace layout）
- [ ] 3.4 `useWorkspaceNav.ts`：「管理后台」仅 `isAdmin` 时展示
- [ ] 3.5 登录后 / 有 token 进 `/app` 时拉取 permission info（含 `isAdmin`）
- [ ] 3.6 `cd web && pnpm build`

## 4. 联调与门禁

- [ ] 4.1 `spring-boot:run` + `pnpm dev`：普通成员（零角色）不可进 `/admin`、不见入口；`admin` 可进
- [ ] 4.2 未登录访问 `/app/messages`、`/admin` → `/app/login`
- [ ] 4.3 `openspec validate admin-portal-gate --strict`

## 不在本 change

- Flyway / 新表
- `/app-api/**` RBAC
- 客户端组织可见性策略
