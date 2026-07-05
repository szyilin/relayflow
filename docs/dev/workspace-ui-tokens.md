# 员工工作台 UI 设计 Token

> **来源**：`app-workspace` 原型验收版，对照 `web/src/assets/css/workspace.css` 与 `components/workspace/`。  
> 真源代码变更时须同步更新本文档。

## 视觉方向

**客户端分层（飞书 / Discord 气质）** — 画布底 + 圆角卡片列，与管理端 `B · Clean Enterprise` **独立**，禁止混用 `--admin-*`。

| 项 | 值 |
|----|-----|
| 主色 | 与管理端共用 Nuxt UI **teal**（`--ui-primary`） |
| 画布 | `--ws-canvas-bg`（列与列之间的间隙底色） |
| 卡片 | 每列独立圆角 + 轻阴影 |
| 默认主题 | 与管理端共用 `useAdminColorMode` |

## CSS Token（`web/src/assets/css/workspace.css`）

| Token | 浅色 | 深色 | 用途 |
|-------|------|------|------|
| `--ws-canvas-bg` | `#e8ebf0` | `#0b0c0e` | 整体背景 |
| `--ws-rail-bg` | `#f3f5f8` | `#151619` | 左侧导航卡片 |
| `--ws-panel-bg` | `#ffffff` | `#1c1d21` | 列表栏卡片 |
| `--ws-main-bg` | `#ffffff` | `#222327` | 主工作区卡片 |
| `--ws-aside-bg` | `#ffffff` | `#1c1d21` | 右侧栏卡片 |
| `--ws-rail-w` | `13.5rem` | 同左 | 导航固定宽度 |
| `--ws-shell-gap` | `0.625rem` | 同左 | 卡片间距 |
| `--ws-card-radius` | `0.875rem` | 同左 | 圆角 |
| `--ws-text` / `--ws-text-muted` | 见 CSS | 见 CSS | 文字 |

## 语义用法

| 用途 | 做法 | 禁止 |
|------|------|------|
| 壳层 | `WorkspaceShell` + `workspace-card` | 使用 `UDashboardSidebar` |
| 内部分割 | `border-[var(--ws-border-subtle)]` | 列间硬 `border-right` 满高 |
| 列表选中 | `.workspace-list-item[data-active='true']` | 管理端 active 样式 |
| 搜索框 | `.workspace-search` | 裸 `UInput` 无容器样式 |

## 可拖拽列表栏

- 中间 `#panel` 列宽由 `useWorkspacePanelResize` 控制（240–480px）
- 拖拽手柄：`WorkspaceResizeHandle`（列表右缘）
- 宽度持久化：`localStorage` key `relayflow:ws:panel-width`

## 参考

- [workspace-ui-patterns.md](workspace-ui-patterns.md) — 页面模式
- [admin-ui-tokens.md](admin-ui-tokens.md) — 管理端 token（勿混用）
