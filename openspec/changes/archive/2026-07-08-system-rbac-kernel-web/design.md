# 设计：RBAC 内核 — 前端（system-rbac-kernel-web）

## Context

- 登录走 `POST /admin-api/system/auth/login`；token 存 `stores/auth`
- 后端就绪后提供 `GET /admin-api/system/auth/get-permission-info`
- 契约：[`openspec/lanes/system-rbac-kernel/contract.md`](../../lanes/system-rbac-kernel/contract.md)

## Goals / Non-Goals

**Goals:**

- 登录成功 → `fetchPermissionInfo()`
- 刷新页面 / 直访 `/admin` → 有 token 则拉 permissions
- Sidebar 按 contract 映射隐藏无权限项
- `logout` 清空 permissions

**Non-Goals:**

- 角色管理页
- 403 专用页（可选；无则依赖 API 错误 toast）

## Decisions

### D1：permission 存储

**决策**：`stores/auth.ts` 内 `permissions: ref<string[]>([])`，**不** persist localStorage（每次进 admin 拉取，避免权限变更后 stale）。

### D2：useAdminNav 过滤

**决策**：`NavigationMenuItem` 扩展可选字段 `permission?: string`；`computed` 过滤 `!permission || hasPermission(permission)`。

新增 **角色管理** nav 占位（指向将来 `/admin/system/role`，可先链到占位或隐藏到 role slice 再加 — contract 要求映射，应加 nav 项但 route 可 404  until admin-role-slice — better add nav item with permission `system:role:list` pointing to a simple placeholder page OR just add to nav array for when role page exists).

Design says add role nav with permission - route doesn't exist yet. Options:
1. Add nav item to `/admin/system/role` - need a minimal placeholder page
2. Only filter existing nav items without adding role yet

Contract says:
| 角色管理 | system:role:list |

I'll specify in web design: add nav entry to `/admin/system/role` with a minimal placeholder page (UEmpty "角色管理将在下一切片接入") OR skip page and only add nav when admin-role-slice - user said frontend connects rbac only. I'll say add nav item + minimal placeholder page to avoid 404 - actually that might be scope creep.

Safer: **本切片仅给现有 nav 项加 permission 过滤**；角色管理 nav **不在本切片添加**（contract 映射表写「下一切片 admin-role-slice 添加 nav」）。 Update contract? User frozen contract includes role nav mapping - web can add nav item that points to `/admin/system/role` with permission - if page missing, add minimal stub.

I'll tasks: add role nav with permission, create minimal `pages/admin/system/role/index.vue` placeholder - that's small and matches contract.

### D3：nickname

**决策**：`fetchPermissionInfo` 后更新 `user.nickname` 为 API 返回值。

## 实现要点

```typescript
// composables/usePermission.ts
export function usePermission() {
  const auth = useAuthStore()
  function hasPermission(code: string) {
    return auth.permissions.includes(code)
  }
  return { hasPermission }
}
```

```typescript
// layouts/admin.vue or App - onMounted
if (auth.isAuthenticated) await auth.fetchPermissionInfo()
```

Nav permissions per contract:
- 用户管理 → `system:user:list`
- 部门管理 → `system:dept:list`

## 验证

```bash
cd web && pnpm build
```

浏览器：

1. `admin` 登录 → sidebar 见用户/部门
2. （若后端可测）减权限后刷新 → 对应项隐藏

## 依赖后端

**必须**后端 `get-permission-info` 可用后再开始本 change。
