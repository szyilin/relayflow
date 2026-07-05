# 提案：RBAC 内核 — 后端（system-rbac-kernel-api）

## Why

`system-schema-v1` 已建权限表与种子，但运行时仅校验 JWT，`LoginUser` 无 authorities，管理端 API 未按 `permission.code` 拦截。须在后端落地 Permission 加载、方法级鉴权与权限信息 API，供前端门禁对接。

## What Changes

- `PermissionService`：按 userId + tenantId 加载 roles 与 permission codes
- `JwtAuthenticationFilter` / `LoginUser`：填充 `GrantedAuthority`
- `@EnableMethodSecurity` + `@PreAuthorize("hasAuthority('…')")`
- `GET /admin-api/system/auth/get-permission-info`
- `DataScopeHelper` 骨架（供后续 user 列表过滤）
- 回归：`user/page` → `system:user:list`；`user/create` 移出 permitAll 并加 `system:user:create`
- 冻结 [`openspec/lanes/system-rbac-kernel/contract.md`](../../lanes/system-rbac-kernel/contract.md)

## Capabilities

### New Capabilities

（无新域）

### Modified Capabilities

- `system`：RBAC 运行时 enforcement、Permission info API

## Impact

| 区域 | 影响 |
|------|------|
| `relayflow-framework/relayflow-spring-boot-starter-security` | Method security、403、LoginUser authorities |
| `relayflow-module-system-biz` | PermissionService、AuthController、UserController 注解 |
| `web/` | **不改**（→ `system-rbac-kernel-web`） |

## 不在本 change

- 部门/角色 CRUD API
- 前端 store / nav
- user/page data_scope 过滤（→ `admin-user-mutate-slice`）

## 前置

- 史诗：[`system-admin-v1`](../system-admin-v1/design.md)
- 契约：[`openspec/lanes/system-rbac-kernel/contract.md`](../../lanes/system-rbac-kernel/contract.md)
