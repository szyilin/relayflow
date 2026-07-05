# 设计：admin-shell-web

## API 契约（只读）

见 [`_lanes/admin-shell/contract.md`](../_lanes/admin-shell/contract.md)。

## 技术选型

| 层 | 选型 |
|----|------|
| API | `web/src/api/admin/tenant.ts` → `getDefaultTenant()` |
| Store | Pinia `useTenantStore`：`tenantName`，`fetchDefaultTenant()` |
| 触发时机 | `layouts/admin.vue` onMounted 或 store 首次使用时 |
| UI | 已有 `AdminNavbar` 绑定 `tenantStore.tenantName` |
| 退出 | 已有 `AdminUserMenu` → `authStore.logout()` |

## 错误与 fallback

- API 失败：`tenantName` fallback 为「默认企业」，可选 `useToast` 提示
- 不阻断壳层渲染

## 与 -api 的边界

后端 Security/CORS 问题 **不在本 change 修**；记录到 `-integrate` 或反馈 `-api` lane。

## 验证（本 lane）

```bash
cd web && pnpm build
```

浏览器路径在 **`admin-shell-integrate`** 中验收。
