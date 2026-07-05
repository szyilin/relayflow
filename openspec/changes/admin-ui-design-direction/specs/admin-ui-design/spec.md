## ADDED Requirements

### Requirement: 管理端视觉方向文档化

项目 MUST 在实现管理端业务页面前，通过 OpenSpec change 文档化一种选定的管理端视觉方向（三种候选：Slate Command、Clean Enterprise、Soft Glass），且该方向 MUST 涵盖色彩、字体、布局壳层与核心页面模式。

#### Scenario: 方向选定前不开始 login 切片 UI 实现

- **WHEN** 管理端视觉方向尚未在 change design 中经产品负责人确认
- **THEN** `admin-login-slice` 的登录页 UI 实现 MUST NOT 作为正式交付开始
- **AND** 可先进行与视觉无关的后端联调（CORS、curl）

#### Scenario: 方向确认记录

- **WHEN** 产品负责人选定视觉方向（A/B/C 或 B 的变体）
- **THEN** 选定结果 MUST 写入 `openspec/changes/admin-ui-design-direction/design.md` 的 Open Questions 关闭项或 Decision 表
- **AND** 后续管理端页面 MUST 遵循该方向

---

### Requirement: 设计 Token 统一

管理端 `web/` MUST 通过 Tailwind v4 `@theme` 与 Nuxt UI semantic 变量定义统一设计 token，包括主色、语义色（success/warning/error）、圆角与 sans 字体栈；业务页面 MUST NOT 使用硬编码 hex 色作为常规样式手段。

#### Scenario: 主题变量集中定义

- **WHEN** 开发者查看 `web/src/assets/css/main.css`
- **THEN** MUST 存在 `@theme static` 块定义 `--font-sans` 与 primary 色阶
- **AND** 页面组件 SHOULD 使用 `text-primary`、`bg-elevated`、`border-default` 等 semantic class

#### Scenario: 明暗色模式

- **WHEN** 用户切换 color mode（明/暗）
- **THEN** 管理端壳层与卡片 MUST 在两种模式下均可读
- **AND** 默认模式 MUST 与选定视觉方向一致（Light 或 Dark 优先）

---

### Requirement: 管理端壳层布局

管理端路由 `/admin/*`（登录页除外）MUST 使用统一壳层：可折叠侧边栏、顶栏（标题/租户/用户菜单）、内容区统一内边距；导航 MUST 仅展示 RelayFlow 真实模块入口，不得将 dashboard-vue 演示页作为产品菜单项。

#### Scenario: 壳层结构

- **WHEN** 已登录用户访问 `/admin` 或子路由
- **THEN** 页面 MUST 渲染 sidebar + navbar + body 三区布局
- **AND** MUST 使用 Nuxt UI dashboard 原语（如 `UDashboardGroup`、`UDashboardSidebar`、`UDashboardNavbar`）

#### Scenario: 登录页无壳层

- **WHEN** 用户访问 `/admin/login`
- **THEN** 页面 MUST NOT 显示管理端 sidebar
- **AND** MUST 使用独立认证布局

---

### Requirement: 页面模式规范

管理端 MUST 为以下页面类型定义并实现一致的 UI 模式：认证页、概览页、列表页、表单页、空状态；各模式 MUST 映射到指定 Nuxt UI 组件组合（见 design.md 页面模式表）。

#### Scenario: 列表页模式

- **WHEN** 实现管理端数据列表（如用户列表）
- **THEN** 页面 MUST 包含顶栏、筛选区、`UTable` 与分页
- **AND** 表格行 hover MUST 使用 elevated 背景而非自定义高亮色块

#### Scenario: 空状态

- **WHEN** 列表或面板无数据
- **THEN** MUST 使用 `UEmpty` 或等效 Nuxt UI 模式
- **AND** MUST 提供明确的主操作引导按钮

---

### Requirement: 组件与交互克制

管理端 UI MUST 以 Nuxt UI v4 组件为主实现；图标 MUST 使用 Lucide（`i-lucide-*`）；交互 MUST 尊重 `prefers-reduced-motion`，且 MUST NOT 在页面级使用大面积 decorative 动画或 scale 动效。

#### Scenario: 主操作按钮

- **WHEN** 页面存在主操作（提交、登录、创建）
- **THEN** MUST 使用 `UButton` `color="primary"`
- **AND**  destructive 操作 MUST 经 `UModal` 或等效确认

#### Scenario: 反馈

- **WHEN** API 操作成功或失败
- **THEN** MUST 通过 toast 或 inline alert 反馈
- **AND** MUST NOT 仅依赖 `console.log`

---

### Requirement: 设计预览（可选）

若 change tasks 启用设计预览页，项目 MAY 提供 `/admin/design-preview` 路由，用于展示 token 与常用组件组合；该路由 MUST NOT 出现在生产导航菜单，且 MAY 在 V1 后期移除。

#### Scenario: 预览页隔离

- **WHEN** 设计预览页存在
- **THEN** 侧边栏导航 MUST NOT 链接至该页
- **AND** 该页 MUST 仅用于开发期视觉验收
