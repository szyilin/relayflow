# 设计：管理后台门户准入（admin-portal-gate）

## Context

- **约定真源**：[`docs/dev/product-permission-model.md`](../../docs/dev/product-permission-model.md)
- **现状**：
  - `get-permission-info` 返回 `roles`、`permissions`，无 `isAdmin`
  - `SecurityAutoConfiguration`：`/admin-api/**` 仅要求 `authenticated()`，零 permission 用户可调多数端点（具体端点另有 `@PreAuthorize`，但 tenant/default、get-permission-info 等仍可达）
  - 前端 `router/guards.ts`：`/admin/**` 只验 token；工作台「管理后台」链接全员可见
- **约束**：同一 JWT；**不**新增登录页；**不**用 RBAC 限制 `/app` 产品面

## Goals / Non-Goals

**Goals:**

- 闭环 L0 **门户准入**：`isAdmin` 判定一致（后端真源 + 前端消费）
- 非管理员无法进入 `/admin` UI 与（除白名单外）`/admin-api`
- 未登录访问 `/app/**`（除 login）、`/admin/**` 一律 redirect login
- 契约与 spec delta 可 archive 至 `openspec/specs/`

**Non-Goals:**

- 客户端协作/可见性策略
- `/app-api/**` 按 `sys_permission` 门禁
- 飞书式可配置无权限引导页

## Decisions

### D1：`isAdmin` 判定（后端真源）

**决策**：`isAdmin = 当前 tenant 下用户有效 permission codes 非空`（即 `user → sys_user_role → sys_role → sys_role_permission → sys_permission` 并集后 `permissions.size() > 0`）。

**理由**：与 `product-permission-model.md` §2.2「至少一个管理角色且角色含 ≥1 permission」等价；实现可复用已有 `PermissionService.getPermissionCodes()`。

**替代**：单独查「是否有 role 记录」— 拒绝，因零 permission 的角色不应算管理员。

### D2：API 暴露

**决策**：`GET /admin-api/system/auth/get-permission-info` 增加 `data.isAdmin: boolean`（与 `permissions` 一致推导，不单独缓存）。

**理由**：登录后与工作台入口、路由守卫共用；任意已登录成员仍可调用（便于前端区分成员 vs 管理员）。

### D3：管理面 API 门户 Filter

**决策**：在 `relayflow-spring-boot-starter-security` 增加 `AdminPortalAuthorizationFilter`（或等价），匹配 `/admin-api/**`：

| 条件 | 行为 |
|------|------|
| 白名单路径 | 跳过（见 D4） |
| 未认证 | 401（现有 EntryPoint） |
| 已认证且 authorities 为空 | **403** |
| 已认证且 authorities 非空 | 继续，各端点 `@PreAuthorize` 仍生效 |

**理由**：门户层与功能 permission 分层；防止零权限用户调用仅 `authenticated()` 的端点。

### D4：白名单（permitAll + 零权限可访问）

| 路径 | 原因 |
|------|------|
| `POST /admin-api/system/auth/login` | 登录 |
| `GET /admin-api/system/tenant/default` | 登录页/壳层展示租户名（产品面也可能用） |
| `GET /admin-api/system/auth/get-permission-info` | 须让非 admin 成员拉取 `isAdmin=false` |

其余 `/admin-api/**` 均须 `isAdmin`（authorities 非空）。

### D5：前端路由

**决策**：

1. **未登录**：`/app/**`（除 `/app/login`）、`/admin/**` → `/app/login?redirect=...`（保持现有逻辑，验收补全边缘路由）
2. **已登录非 admin 访问 `/admin/**`** → `/app/no-admin-access`（workspace layout 内静态引导页，**不**挂载 `admin` layout）
3. **`useWorkspaceNav` footerLinks**：`isAdmin` 为 true 时才含「管理后台」
4. **登录成功**：已有 `fetchPermissionInfo()`；store 增加 `isAdmin` computed/ref

**替代**：redirect 直接回 `/app/messages` — 拒绝，用户无反馈；独立页更清晰。

### D6：单 change 前后端同批

**决策**：一个 `admin-portal-gate` change，tasks 顺序 **后端 → 前端 → 联调**（非 `-web` 先行，因行为依赖 `isAdmin` API）。

**理由**：门户是 cross-cutting；前端无法 Mock 长期真源。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 仅绑角色未绑 permission 的测试用户进不了后台 | 符合产品定义；种子 `super_admin` 仍可用 |
| `tenant/default` 对白名单开放 | 已存在行为；不泄露管理数据 |
| 与未 archive 的 `system-rbac-kernel-*` 重叠 | 本 change 只增门户层，不重复 kernel tasks |

## Migration Plan

- 无 Flyway；部署后即生效
- 回滚：移除 Filter + 前端守卫即可恢复旧行为

## Open Questions

- 无（V1 无权限页文案先用静态中文）
