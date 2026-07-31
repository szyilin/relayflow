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

### 语义 / 身份 tint（`--ws-tint-*`）

明暗 **同 hue 族**，仅调亮度/透明度。业务页禁止散落 hex；标签走 `WorkspaceSemanticBadge`。

| Token 族 | 用途 |
|----------|------|
| `info` / `success` / `warning` / `danger` | 通用状态 |
| `official` / `bot` / `agent` / `external` / `role` | 身份 |
| `coming-soon` | 产品态「即将推出」 |
| 每族另有 `-muted` | soft 标签底色 |

### 模块 tint（Rail 图标）

| 变量 | 模块 |
|------|------|
| `--ws-module-messages` | 消息 |
| `--ws-module-contacts` | 通讯录 |
| `--ws-module-docs` | 云文档 |
| `--ws-module-calendar` | 日历 |
| `--ws-module-tasks` | 任务 |
| `--ws-module-meeting` | 视频会议 |
| `--ws-module-agents` | 智能体 |

Rail 项通过 `--ws-module-tint` 注入；图标 class `.workspace-rail-icon`。文字保持 `--ws-text` / muted，不整行彩虹。

### 默认字母头像渐变

- util：`avatarGradientFromId` / `letterAvatarStyle`（`web/src/utils/avatar.ts`）
- 组件：`WorkspaceAvatar`（有 `src` 用图；无图则柔和同色相渐变 + 首字）
- 做法：浅色柔和 pastel（两端都偏亮，色差小）；三档对角渐变；**禁止**底部沉到藏青/炭灰 + 白色高光罩（易显老气）
- seed 优先 `userId`，否则名称；同 seed 跨页同色

## 语义用法

| 用途 | 做法 | 禁止 |
|------|------|------|
| 壳层 | `WorkspaceShell` + `workspace-card` | 使用 `UDashboardSidebar` |
| 内部分割 | `border-[var(--ws-border-subtle)]` | 列间硬 `border-right` 满高 |
| 列表选中 | `.workspace-list-item[data-active='true']` | 管理端 active 样式 |
| 搜索框 | `.workspace-search` | 裸 `UInput` 无容器样式 |
| 身份/状态色 | `--ws-tint-*` + `WorkspaceSemanticBadge` / `WorkspaceAvatar` | 染画布/列卡片/正文框 |
| 空态 | `WorkspaceEmptyState` | 假数据填空；仅 muted 灰字无锚点 |

## 可拖拽列表栏

- 中间 `#panel` 列宽由 `useWorkspacePanelResize` 控制（240–480px）
- 拖拽手柄：`WorkspaceResizeHandle`（列表右缘）
- 宽度持久化：`localStorage` key `relayflow:ws:panel-width`

## 参考

- [workspace-ui-patterns.md](workspace-ui-patterns.md) — 页面模式
- [workspace-illustrations.md](workspace-illustrations.md) — 空态等插画：Storyset 取图、落仓与官网调整
- [admin-ui-tokens.md](admin-ui-tokens.md) — 管理端 token（勿混用）
