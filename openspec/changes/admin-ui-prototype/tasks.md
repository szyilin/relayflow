# 任务：admin-ui-prototype

> **性质**：UI 先行 — Mock 全壳层原型，**零后端**。视觉基调见 `admin-ui-design-direction`（B · Clean Enterprise，已确认）。  
> 验收通过后再做 `admin-login-slice` 换真 API。

## 切片：Mock 管理端全框架

### 基础（web/）

- [ ] 1.1 确认/添加 Pinia；新建 `stores/auth.ts`、`stores/tenant.ts`（Mock 读写 `localStorage`）
- [ ] 1.2 新建 `mocks/`（auth、tenant、dashboard、system/users）
- [ ] 1.3 更新 `main.css` token（B 方向 teal + Inter）；color mode 跟随系统
- [ ] 1.4 实现路由守卫（`router/guards.ts` 或 `main.ts` beforeEach）

### 布局（web/）

- [ ] 2.1 新建 `layouts/auth.vue`（登录左右分栏）
- [ ] 2.2 增强 `layouts/admin.vue`：`useAdminNav` 分组菜单、租户 badge、UserMenu 退出
- [ ] 2.3 新建 `components/admin/`（AdminNavbar、AdminUserMenu、AdminPageHeader）

### 页面占位（web/）

- [ ] 3.1 `pages/admin/login.vue` — Mock 登录表单
- [ ] 3.2 `pages/admin/index.vue` — 概览统计卡片 + 快捷入口
- [ ] 3.3 `pages/admin/system/user/index.vue` — Mock 表格、筛选、分页
- [ ] 3.4 `pages/admin/system/user/create.vue` — 表单占位 + toast
- [ ] 3.5 `pages/admin/system/dept/index.vue` — 空状态/骨架树占位
- [ ] 3.6 `pages/admin/infra/file/index.vue` — 表格占位
- [ ] 3.7 `pages/admin/design-preview.vue` — 组件/token 板

### 清理（web/）

- [ ] 4.1 移除或隔离模板 demo 页（customers、inbox、settings 等）
- [ ] 4.2 根路径 `/` 重定向或引导至 `/admin/login`

### 验证

- [ ] 5.1 `cd web && pnpm build`
- [ ] 5.2 浏览器走查：登录 → 概览 → 各 sidebar 页 → 退出 → 未登录拦截（路径见 design.md）
- [ ] 5.3 `openspec validate admin-ui-prototype --strict`

## 不在本 change

- `api/admin/*`、Vite proxy、`spring-boot:run`
- `admin-login-slice`（下一阶段替换 Mock auth）

## 后续衔接

| Change | 说明 |
|--------|------|
| `admin-login-slice` | 保留页面/壳层，store 改调真 API |
| `admin-shell-slice` | tenant store 接 API |
| `admin-user-list-slice` | 用户列表接分页 API |
