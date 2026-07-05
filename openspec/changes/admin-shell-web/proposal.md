# 提案：管理端壳层 · 前端 Lane（admin-shell-web）

## 背景

`admin-ui-prototype` 壳层 navbar 已用 Mock 租户名。本 change 为 **并行 Lane 之前端部分**：将 `stores/tenant.ts` 接真 API，**保留** navbar / 退出交互。

## 并行结构

| Change | Lane |
|--------|------|
| `admin-shell-api` | 后端 |
| **admin-shell-web**（本 change） | 前端 |
| `admin-shell-integrate` | 联调与门禁 |

契约真源：[`openspec/changes/_lanes/admin-shell/contract.md`](../_lanes/admin-shell/contract.md)

## 前置条件

- [x] `admin-ui-prototype` UI 定调通过
- [x] `admin-login-slice` 已完成
- [x] `_lanes/admin-shell/contract.md` 已冻结

## 范围

| 模块 | 内容 |
|------|------|
| `web/src/api/admin/tenant.ts` | 新增，调用 `/default` |
| `web/src/stores/tenant.ts` | Mock → 真 API |
| `web/src/components/admin/AdminNavbar.vue` | 仅必要时接线（预计 store 即可） |
| `AdminUserMenu.vue` | 确认退出流程（清 token + 跳转） |
| Java 模块 | **不修改** |

## 非目标

- 重做壳层 layout / 主题
- 租户切换、多租户 UI
- 服务端 logout

## 用户可见结果

登录后 `/admin` navbar 显示 **真实** 租户名（非 Mock「默认企业」硬编码）；退出仍回 `/admin/login`。
