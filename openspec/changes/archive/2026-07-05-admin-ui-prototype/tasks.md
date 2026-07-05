# 任务：admin-ui-prototype

> **性质**：管理端 UI **可点击原型**（阶段 2）— Mock 全壳层，**零后端**；目的是 **确认展示效果**，供下一阶段 **反抽工程化规则**。  
> 全流程见 [`docs/dev/admin-ui-workflow.md`](../../docs/dev/admin-ui-workflow.md)。视觉大方向：B · Clean Enterprise（阶段 0 已确认）。

## 阶段 1：壳层骨架（web/）

- [x] 1.1 新建 `layouts/auth.vue`（登录左右分栏，无 sidebar）
- [x] 1.2 增强 `layouts/admin.vue`：sidebar 分组占位、navbar 右侧用户区占位、active 导航样式
- [x] 1.3 新建 `composables/useAdminNav.ts`（sidebar 单源）
- [x] 1.4 新建 `components/admin/`（AdminNavbar、AdminUserMenu、AdminPageHeader）
- [x] 1.5 验证：浏览器打开 `/admin`（Mock 登录前可先临时跳过守卫查看壳层）

## 阶段 2：设计 token（web/）

- [x] 2.1 更新 `web/src/assets/css/main.css`（B 方向 teal + Inter、`@theme`、圆角）
- [x] 2.2 配置 color mode **跟随系统**（`app.config.ts` 或 Nuxt UI 等效）
- [x] 2.3 验证：Light/Dark 切换；`cd web && pnpm build`

## 阶段 3：组件板（web/）

- [x] 3.1 新增 `pages/admin/design-preview.vue`：色板、按钮、表单、表格、空状态、Skeleton
- [x] 3.2 验证：浏览器 `/admin/design-preview`（不进 sidebar 产品菜单，可 footer dev 链）

## 阶段 4：代表页占位（web/）

- [x] 4.1 `pages/admin/login.vue` — Mock 登录表单（auth layout）
- [x] 4.2 `pages/admin/index.vue` — 概览统计卡片 + 快捷入口
- [x] 4.3 `pages/admin/system/user/index.vue` — Mock 表格、筛选、分页
- [x] 4.4 `pages/admin/system/user/create.vue` — 表单占位 + toast
- [x] 4.5 `pages/admin/system/dept/index.vue` — 空状态或骨架树
- [x] 4.6 `pages/admin/infra/file/index.vue` — 表格占位

## 阶段 5：Mock 态与守卫（web/）

- [x] 5.1 确认/添加 Pinia；`stores/auth.ts`、`stores/tenant.ts`（读写 `localStorage`，封装 mocks）
- [x] 5.2 新建 `mocks/`（auth、tenant、dashboard、system/users）；字段名对齐未来 API VO
- [x] 5.3 实现路由守卫（`router/guards.ts` 或 `main.ts` beforeEach）
- [x] 5.4 验证：任意账号登录 → `/admin`；退出 → 登录；直访 `/admin/system/user` → 拦截

## 阶段 6：清理（web/）

- [x] 6.1 移除或隔离模板 demo 页（customers、inbox、settings 等）
- [x] 6.2 根路径 `/` 重定向或引导至 `/admin/login`

## 阶段 7：构建与浏览器走查

- [x] 7.1 `cd web && pnpm build`
- [x] 7.2 浏览器走查（路径见 `design.md`「验收标准」）：登录 → 概览 → sidebar 各页 → design-preview → 退出 → 拦截
- [x] 7.3 `openspec validate admin-ui-prototype --strict`

## 阶段 8：人工验收门（阻塞规则沉淀）

- [x] 8.1 **你确认 UI 定调**：在 `admin-ui-design-direction/design.md`「验收记录」填写日期、commit、结论「UI 定调通过」
- [ ] 8.2 若需迭代：在本 change 修正后重复 7.x，直至 8.1 通过

> **本 change 完成标志 = 8.1 签字**，不是仅 build 通过。

## 不在本 change

- `api/admin/*`、Vite proxy、`spring-boot:run`
- `docs/dev/admin-ui-tokens.md`、`admin-ui-patterns.md`、`.cursor/rules/admin-ui-patterns.mdc`（→ `admin-ui-design-direction` 阶段 2–4）
- `admin-login-slice`（规则沉淀后再做）

## 后续衔接

| Change | 前置 |
|--------|------|
| `admin-ui-design-direction`（规则沉淀） | 本 change **8.1 签字** |
| `admin-login-slice` | 规则文档 + spec 就绪；保留本 change 页面与壳层 |
