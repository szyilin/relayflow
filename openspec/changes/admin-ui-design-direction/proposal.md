# 提案：管理端 UI 视觉基调（admin-ui-design-direction）

## Why

RelayFlow 即将进入纵向切片阶段（登录、壳层、用户管理等），当前 `web/` 仅继承 dashboard-vue 模板默认样式（Public Sans + 绿色主题），**尚未定义产品级视觉语言**。后端是项目能力展示的核心，但管理端界面若缺乏统一、现代、专业的观感，会削弱整体产品印象，也不利于后续切片在一致的设计系统上快速迭代。

在 `admin-login-slice` 等对接工作开始前，应先通过 OpenSpec 明确：**管理端应呈现何种气质、使用哪些设计 token、页面骨架与组件模式如何统一**，供你确认后再落地到代码。

## What Changes

- 新增管理端 **UI 设计方向** 规格（视觉气质、色彩、字体、布局、组件模式、动效原则）
- 在 `design.md` 中提供 **三种可选视觉方向**（含对比表与推荐），供你选定一种作为 V1 基调
- 定义 **设计 token 与页面模式**（登录页、Dashboard 壳层、列表页、表单页、空状态）的规范，不引入新 UI 框架（仍基于 Nuxt UI v4）
- 规划 **设计落地任务**：主题 token、壳层调整、设计预览页（可选）、与后续 `admin-login-slice` 的衔接
- **不涉及**后端 API 变更、Flyway、新业务功能逻辑

## Capabilities

### New Capabilities

- `admin-ui-design`：RelayFlow 管理端（`/admin/*`）的视觉基调、设计 token、布局与组件使用规范

### Modified Capabilities

- （无）— 不修改 `deployment`、`system` 等领域行为规格；仅在归档时将 `admin-ui-design` 合入主 specs

## Impact

| 范围 | 影响 |
|------|------|
| `web/` | `main.css` 主题 token、`layouts/admin.vue`、后续 `pages/admin/*` 与共享组件 |
| Java / deploy | 无 |
| 依赖 | 无新增 npm 包（V1 限制在 Nuxt UI + Tailwind v4 能力内） |
| 自部署 | 纯静态样式与 Vue 组件，无迁移；回滚 = 还原 CSS/布局文件 |
| 后续 change | `admin-login-slice`、`admin-shell-slice` 等切片须遵循本 change 确定的方向 |
