# 任务：admin-ui-design-direction

> **性质**：设计基调 change —— 在 `admin-login-slice` 之前完成视觉方向确认与 token/壳层落地。  
> 结构见 [`docs/dev/vertical-slice-workflow.md`](../../docs/dev/vertical-slice-workflow.md)（本 change 无后端 API）。

## 阶段 0：方向确认（人工 — 阻塞后续）

- [x] 0.1 阅读 `design.md` 三种方向 —— **已选 B Clean Enterprise**
- [x] 0.2 已确认：主题跟随系统、登录左右分栏、启用 design-preview（见 design.md「已确认决策」）

## 阶段 1：设计 Token 与字体（web/）

- [ ] 1.1 按选定方向更新 `web/src/assets/css/main.css`（`@theme` primary、字体栈、圆角）
- [ ] 1.2 配置 color mode 默认策略（`app.config.ts` 或 Nuxt UI 等效配置）
- [ ] 1.3 验证：`cd web && pnpm build`

## 阶段 2：壳层与品牌（web/）

- [ ] 2.1 调整 `layouts/admin.vue`：sidebar 分组结构占位、Navbar 右侧用户区占位、active 导航样式
- [ ] 2.2 统一品牌区：Logo 图标 + RelayFlow 文案（与 design 一致）
- [ ] 2.3 隔离或移除 dashboard 演示页路由（customers/inbox/settings 等），避免进入 `/admin` 菜单
- [ ] 2.4 验证：`pnpm build`；浏览器打开 `/admin` 检查壳层

## 阶段 3：设计预览页（可选 — 任务 0.1 若启用）

- [ ] 3.1 新增 `pages/admin/design-preview.vue`：展示色板、按钮、表单、表格、空状态示例
- [ ] 3.2 验证：浏览器 `/admin/design-preview`（不加入 sidebar 菜单）

## 阶段 4：文档与门禁

- [ ] 4.1 将选定方向摘要写入 `docs/dev/` 或归档前保留于 design（≤1 页设计速查）
- [ ] 4.2 `openspec validate admin-ui-design-direction --strict`

## 后续衔接（不在本 change 范围）

- **`admin-ui-prototype`**（下一优先）：Mock 全壳层 + 占位页，浏览器验收整体效果；**本 change 仅保留设计规格**
- `admin-login-slice`：原型验收后，保留页面结构，替换 Mock 为真 API
- `admin-shell-slice`：租户名、退出登录接 API
