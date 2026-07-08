# 提案：RBAC 内核 — 前端（system-rbac-kernel-web）

## Why

后端 `system-rbac-kernel-api` 落地后，管理端须拉取 `get-permission-info` 并按 `permission.code` 过滤 sidebar，否则 UI 与 API 403 行为不一致。

## What Changes

- `api/admin/auth.ts`：`getPermissionInfo()`
- `stores/auth.ts`：`permissions`、`fetchPermissionInfo()`；登录后拉权限
- `composables/usePermission.ts`
- `useAdminNav.ts`：菜单项 `permission` + 过滤
- 进入 admin 壳层时 refresh permissions（已有 token）
- `pnpm build`

## Capabilities

### Modified Capabilities

- `web-admin`：管理端权限门禁（sidebar）

## Impact

| 区域 | 影响 |
|------|------|
| `web/` | auth store、nav、composable |
| Java | **不改** |

## 不在本 change

- 部门/角色/用户新页面
- 按钮级 `v-permission` 全站铺开（本切片仅 nav）

## 前置

- **`system-rbac-kernel-api` 已完成**（看板 `api: ready`；curl 通过）
- 契约：[`openspec/lanes/system-rbac-kernel/contract.md`](../../lanes/system-rbac-kernel/contract.md)
