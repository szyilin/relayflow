# 提案：管理端 UI 原型（Mock 全壳层）

## Why

已确认视觉大方向 **B · Clean Enterprise**（`admin-ui-design-direction` 阶段 0），但当前 `web/` 仍混有 dashboard-vue 演示页，且无法在浏览器里评估 **RelayFlow 管理端整体长什么样**。

本 change 的目的 **不是定后端**，而是：

1. 用 Mock 数据搭起 **完整管理端壳层 + 代表页**，供你在浏览器里 **看、点、确认** 展示效果；
2. 你签字确认后，由 `admin-ui-design-direction`（阶段 3）从 **这版代码** 抽取 token、页面模式、组件与目录约定，写入 `docs/dev/` 与 Cursor 规则；
3. 后续 `admin-login-slice` 等纵向切片 **只替换数据层**，不重做 UI。

因此本阶段 **刻意零后端**：不建 `api/admin/*`、不启动 `relayflow-server`。

## What Changes

- 搭建管理端 **完整路由树**（登录、概览、系统、基础设施等占位页），统一 `/admin` 壳层
- 落地 **B 方向设计 token**（`main.css`）、**auth 分栏登录页**、**design-preview** 组件板
- 引入 **`web/src/mocks/`** 与 Pinia store（Mock 登录态、租户、用户列表、概览统计）
- 实现 **Mock 路由守卫**：任意账号可登录 → 浏览各占位页 → 退出
- **移除/隔离** dashboard 模板演示路由（customers、inbox、settings 等）
- **人工验收门**：浏览器走查通过后，你在 design 或 PR 中 **签字确认 UI 定调**

**不涉及**：Java、`/admin-api/*`、Flyway、真实 JWT、规则文档终稿（→ 下一阶段 change）。

## Capabilities

### New Capabilities

- `admin-ui-prototype`：管理端 Mock 原型 — 全壳层、占位页、Mock 数据与前端路由守卫；**UI 定调的可视化真源**

### Modified Capabilities

- （无）— 不修改 `system`/`deployment` 等领域 API 规格

## Impact

| 范围 | 影响 |
|------|------|
| `web/` | layouts、pages/admin/**、mocks、stores、components/admin、main.css |
| Java / deploy | **无** |
| `docs/dev/` | 本 change 不写规则终稿；仅 `admin-ui-workflow.md` 描述流程 |
| 依赖 | Pinia 若未装则加入 |
| 后续 change | `admin-ui-design-direction` 阶段 2–4 **依赖本 change 人工签字**；`admin-login-slice` **依赖规则沉淀完成** |

## 工作流位置

详见 [`docs/dev/admin-ui-workflow.md`](../../docs/dev/admin-ui-workflow.md)。

```text
admin-ui-design-direction（阶段 0 ✅）
  → admin-ui-prototype（本 change — 当前实现）
  → admin-ui-design-direction（规则沉淀）
  → admin-login-slice / …
```
