# 提案：管理端 UI 原型（Mock 全壳层）

## Why

已确认视觉方向 **B · Clean Enterprise**（`admin-ui-design-direction`），但当前 `web/` 仍混有 dashboard-vue 演示页，且缺少 RelayFlow 真实信息架构下的 **完整管理端壳层**。若此时只做登录页或 design-preview，无法评估「整体产品长什么样、模块间如何跳转、列表/表单/概览是否协调」。

因此在本阶段 **刻意不做后端对接**：用 Mock 数据搭起管理端 **整体前端架构 + 占位页面**，先在浏览器里验收交互与视觉，满意后再进入 `admin-login-slice` 等纵向切片换真 API。

## What Changes

- 搭建管理端 **完整路由树**（登录、概览、系统、基础设施等占位页），统一 `/admin` 壳层
- 引入 **`web/src/mocks/`** 与 Pinia store（Mock 登录态、租户、用户列表、概览统计）
- 实现 **Mock 路由守卫**：任意账号可登录 → 浏览各占位页 → 退出
- 按 B 方向落地 **设计 token**、**auth 分栏登录页**、**design-preview** 组件板
- **移除/隔离** dashboard 模板演示路由（customers、inbox、settings 等）
- **不涉及** Java、`/admin-api/*` 调用、Flyway、真实 JWT

## Capabilities

### New Capabilities

- `admin-ui-prototype`：管理端 Mock 原型 — 全壳层、占位页、Mock 数据与前端路由守卫

### Modified Capabilities

- （无）— 不修改 `system`/`deployment` 等领域 API 规格

## Impact

| 范围 | 影响 |
|------|------|
| `web/` | layouts、pages/admin/**、mocks、stores、components/admin、main.css |
| Java / deploy | **无** |
| 依赖 | 无新增（Pinia 若未装则加入，属前端标配） |
| 后续 change | `admin-login-slice` **延后**至原型验收后；届时替换 Mock store/API，保留页面与壳层 |
| 纵向切片 | 本 change 为 **UI 先行例外**；验收通过后恢复「后端 + 前端同批」 |
