# 提案：管理后台门户准入（admin-portal-gate）

## Why

[`docs/dev/product-permission-model.md`](../../docs/dev/product-permission-model.md) 已定义：**员工工作台**（`/app`）对有效组织成员开放且**不按 RBAC 分入口**；**管理后台**（`/admin`）须 **管理身份**（`isAdmin`）才能进入，进入后再用 `permission.code` 细粒度控制。当前实现仍允许「有 JWT 即可进 `/admin` 壳层」，与飞书/钉钉及本产品约定不符，易误导开发与验收。

## What Changes

- 后端定义并暴露 **`isAdmin`**（当前租户下至少一个管理角色且该角色关联 ≥1 个 `sys_permission`）
- `GET /admin-api/system/auth/get-permission-info` 响应增加 `isAdmin`
- **`/admin-api/**` 门户层**：已认证但 `isAdmin=false` 的用户访问非白名单端点 → HTTP 403（JWT  alone 不足）
- 前端 **`/admin/**` 路由**：已登录但非 `isAdmin` → 不得渲染管理端壳层，跳转无权限引导页
- 工作台 **「管理后台」入口**：仅 `isAdmin` 时展示
- 强化 **`/app/**`（除 login）与 `/admin/**` 未登录** → 强制 `/app/login`（禁止未登录渲染壳层）
- 更新 [`openspec/lanes/system-rbac-kernel/contract.md`](../../lanes/system-rbac-kernel/contract.md) 中 permission-info 字段

## Capabilities

### New Capabilities

（无新 spec 域）

### Modified Capabilities

- `web-auth`：产品面/管理面登录门禁（未登录强制跳转）
- `web-admin`：管理后台门户准入、工作台入口、无权限引导
- `system`：`isAdmin` 语义、permission-info 字段、管理面 API 门户拦截

## Impact

| 区域 | 影响 |
|------|------|
| `relayflow-module-system-biz` | `PermissionService`、`AuthPermissionInfoRespVO` |
| `relayflow-framework/.../security` | 管理面 API 门户 Filter 或等价拦截 |
| `web/` | `stores/auth.ts`、`router/guards.ts`、`useWorkspaceNav.ts`、无权限页 |
| `openspec/lanes/system-rbac-kernel/contract.md` | `isAdmin` 字段 |
| Flyway | **无** |
| `deploy/` | **无** |

## 不在本 change

- 客户端策略（组织可见范围、用户组、应用可用范围）
- 用 RBAC 限制 `/app` 主导航
- 自定义无权限引导文案后台配置（V1 静态页即可）

## 前置

- 开发约定：[`product-permission-model.md`](../../docs/dev/product-permission-model.md)
- RBAC 内核：`system-rbac-kernel-api` / `system-rbac-kernel-web`（已实施或并行 archive）
