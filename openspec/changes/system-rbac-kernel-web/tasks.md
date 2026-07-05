# 任务：system-rbac-kernel-web

> **Lane**：前端 · **前置** `system-rbac-kernel-api` 看板 `api: ready`。

## 前置

- [x] 0.1 确认后端 `GET …/auth/get-permission-info` curl 通过
- [x] 0.2 阅读 [`openspec/lanes/system-rbac-kernel/contract.md`](../../lanes/system-rbac-kernel/contract.md)

## 前端（web/）

- [x] 1.1 `api/admin/auth.ts` 增加 `getPermissionInfo()` 与类型
- [x] 1.2 `stores/auth.ts`：`permissions`、`fetchPermissionInfo()`；login 成功后调用；logout 清空
- [x] 1.3 新建 `composables/usePermission.ts`
- [x] 1.4 `useAdminNav.ts`：菜单 `permission` 字段 + 过滤；按 contract 映射用户/部门
- [x] 1.5 admin 壳层 mount 时（已有 token）调用 `fetchPermissionInfo`
- [x] 1.6 `cd web && pnpm build`

## 联调

- [x] 2.1 `spring-boot:run` + `pnpm dev`：登录 → sidebar 显示与 super_admin 权限一致
- [x] 2.2 更新看板 `system-rbac-kernel` → `web: done`
- [x] 2.3 `openspec validate system-rbac-kernel-web --strict`

## 不在本 change

- Java / Flyway
