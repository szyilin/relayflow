# 任务：admin-portal-gate

> **范围**：管理后台门户准入 + `isAdmin` + 登录门禁强化。约定见 [`product-permission-model.md`](../../docs/dev/product-permission-model.md)。

## 前置

- [x] 0.1 阅读本 change 的 `proposal.md`、`design.md`、spec delta
- [x] 0.2 阅读 [`openspec/lanes/system-rbac-kernel/contract.md`](../../lanes/system-rbac-kernel/contract.md)

## 1. 契约

- [x] 1.1 更新 `openspec/lanes/system-rbac-kernel/contract.md`：`get-permission-info` 增加 `isAdmin` 字段与 curl 示例

## 2. 后端

- [x] 2.1 `PermissionService`：提供 `isAdmin(userId, tenantId)`（或与 permission codes 同源）
- [x] 2.2 `AuthPermissionInfoRespVO` + `get-permission-info` 返回 `isAdmin`
- [x] 2.3 `AdminPortalAuthorizationFilter`（或等价）：`/admin-api/**` 零 authorities → 403；白名单见 design D4
- [x] 2.4 `./mvnw -pl relayflow-server -am compile`
- [x] 2.5 curl：`admin` → `isAdmin=true`；构造零权限用户 → `isAdmin=false` 且 `/admin-api/system/user/page` → 403

## 3. 前端（web/）

- [x] 3.1 `api/admin/auth.ts`、`stores/auth.ts`：类型与 `isAdmin` 状态
- [x] 3.2 `router/guards.ts`：已登录非 admin 访问 `/admin/**` → `/app/no-admin-access`；确认未登录 redirect login
- [x] 3.3 新建 `pages/app/no-admin-access.vue`（workspace layout）
- [x] 3.4 `useWorkspaceNav.ts` / `WorkspaceRail.vue`：「管理后台」仅 `isAdmin` 时展示
- [x] 3.5 登录后 / 有 token 进 `/app` 时拉取 permission info（含 `isAdmin`）
- [x] 3.6 `cd web && pnpm build`

## 4. 联调与门禁

- [x] 4.1 curl 联调：`member_gate` 零角色 → `isAdmin=false`、user/page 403；`admin` → `isAdmin=true`
- [x] 4.2 路由守卫：未登录 `/app/**`、`/admin/**` → `/app/login`（代码已实现，浏览器可复验）
- [x] 4.3 `openspec validate admin-portal-gate --strict`

## 不在本 change

- Flyway / 新表
- `/app-api/**` RBAC
- 客户端组织可见性策略

## 浏览器验证

1. 登录 `admin` / `admin123` → 工作台可见「管理后台」，可进 `/admin`
2. 登录 `member_gate` / `Passw0rd!` → 无「管理后台」入口；直访 `/admin` → `/app/no-admin-access`
3. 未登录直访 `/app/messages` 或 `/admin` → `/app/login?redirect=...`
