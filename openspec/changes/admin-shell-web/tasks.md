# 任务：admin-shell-web

> **Lane**：前端 · 与 `admin-shell-api` 并行；联调见 `admin-shell-integrate`。  
> 契约：[`openspec/lanes/admin-shell/contract.md`](../../lanes/admin-shell/contract.md)

## 前置

- [x] 0.1 `admin-ui-prototype` UI 定调通过
- [x] 0.2 `admin-login-slice` 已完成
- [x] 0.3 `openspec/lanes/admin-shell/contract.md` 已冻结
- [x] 0.4 [`api-integration-board.md`](../../../docs/dev/api-integration-board.md) 该切片 `api: archived`

## 前端（web/）

- [ ] 1.1 新增 `web/src/api/admin/tenant.ts`（`getDefaultTenant`，对齐 contract 字段）
- [ ] 1.2 重构 `stores/tenant.ts`：移除 `mocks/tenant` 依赖，拉取真 API
- [ ] 1.3 在壳层 mount 时调用 `fetchDefaultTenant`（如 `layouts/admin.vue`）
- [ ] 1.4 确认 `AdminUserMenu` 退出：清 token、跳转 `/admin/login`（无回归）
- [ ] 1.5 `cd web && pnpm build`

## 不在本 change

- Java / Security / CORS → `admin-shell-api`
- `spring-boot:run` 浏览器联调 → `admin-shell-integrate`
