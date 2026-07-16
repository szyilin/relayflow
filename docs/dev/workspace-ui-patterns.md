# 员工工作台 UI 页面模式

> **来源**：`web/src/pages/app/` 与 `components/workspace/`。  
> 接 API 时 **只换 store / api 层**，壳层与 token 遵循本文档。

## 目录约定

```text
web/src/
├── layouts/workspace.vue           # 壳层（仅 RouterView）
├── layouts/workspace-auth.vue      # 登录（无侧栏）
├── components/workspace/
│   ├── WorkspaceShell.vue          # 卡片分列壳层
│   ├── WorkspaceRail.vue           # 左导航
│   └── WorkspaceResizeHandle.vue   # 列表栏拖拽
├── composables/
│   ├── useWorkspaceNav.ts          # 导航单源
│   └── useWorkspacePanelResize.ts  # 列表栏宽度
├── assets/css/workspace.css        # --ws-* token（勿与 admin 混用）
├── pages/app/                      # 员工端路由（/app/*）
├── api/app/                        # 用户端 API（后续切片）
└── stores/                         # Pinia（页面不 import mocks/）
```

## 壳层（`/app/*`，登录除外）

**文件**：`WorkspaceShell.vue` + `WorkspaceRail.vue`

| 区域 | 实现 |
|------|------|
| 画布 | `workspace-shell` + `--ws-canvas-bg` + gap |
| 左导航 | 固定宽卡片：品牌、**可点搜索**、图标+文字菜单；顶部 `WorkspaceRailHeader`（头像+昵称同轴） |
| 列表栏 | `#panel` slot；右缘可拖拽调宽 |
| 主区 | 默认 slot；聊天/任务/文档内容 |
| 右栏 | 可选 `#aside`（默认关闭；消息/通讯录不展示「活跃状态」） |
| 主题 | **不**浮在壳层右上角；从资料名片 → **设置窗**切换 |
| 设置窗 | `WorkspaceSettingsPanel`：宽 UModal；左栏分类 + 右栏「通用」 |

## 资料名片与个人入口

| 项 | 规范 |
|----|------|
| 触发 | Rail 点击当前用户头像 → `WorkspaceProfileCard` |
| 头部 | 头像左、昵称+企业名右（同一水平轴）；可编辑昵称、更换头像 |
| 菜单顺序 | 个性签名（占位）、我的个人名片（占位）、登录更多账号、设置、退出登录；管理后台（`isAdmin`）置底 |
| 占位项 | 文案旁 `（占位）`；点击 toast「功能即将推出」 |
| 登录更多账号 | `WorkspaceMoreAccountsPanel`：分组「我的企业」（`my-list`/`switchTenant`）+「本机已登录账号」（Account Dock）；底部「加入企业」占位、「创建新账号」→ `/app/register?addAccount=1` |
| 设置 | 关闭名片后打开独立设置窗（见下）；不在 popover 内嵌迷你设置 |
| 认证页主题 | 登录/注册页不展示主题开关；后续随账号用户配置加载（`user-preference-api`） |

## 工作台设置窗

| 项 | 规范 |
|----|------|
| 入口 | 名片「设置」→ `WorkspaceRailHeader` 打开 `WorkspaceSettingsPanel` |
| 布局 | 左栏分类（账号与安全、通用、隐私、通知、快捷键）；右栏内容；默认「通用」 |
| 通用 | 主题模式（跟随系统/浅色/深色 + 预览）、主题色色点、会话气泡布局（左对齐/左右分布） |
| 数据 | `stores/userPreference`（本地默认 + localStorage；契约见 [`user-preference/contract.md`](../../openspec/lanes/user-preference/contract.md)） |
| 占位分类 | 右侧空态「功能即将推出（占位）」+ toast |

## 全局搜索（⌘K / Rail）

| 项 | 规范 |
|----|------|
| 入口 | Rail 搜索输入点击；工作台页 `⌘K` / `Ctrl+K`（`useWorkspaceSearchShortcut`） |
| UI | `WorkspaceSearchModal`：关键词输入 + `member` / `conversation` / `task` 分组结果 |
| 数据 | `stores/workspaceSearch` → `GET /app-api/infra/workspace-search`（无 Mock） |
| 深链 | `/app/contacts?memberId=`、`/app/messages?conversationId=`、`/app/tasks?taskId=`；目标页读 query 激活上下文 |
| 契约 | [`openspec/lanes/workspace-search/contract.md`](../../openspec/lanes/workspace-search/contract.md) |

## 认证页 `/app/login`

| 项 | 规范 |
|----|------|
| Layout | `meta.layout: workspace-auth` |
| 表单 | 居中 `UCard`；提交走 `useAuthStore().login()` |
| 跳转 | 成功 → `/app/messages`（产品唯一登录页 `/app/login`） |

## 消息页 `/app/messages`

| 项 | 规范 |
|----|------|
| Shell | 不开启 `show-aside`（无「活跃状态」右栏） |
| Panel | 会话列表 + 搜索 + `.workspace-list-item` |
| Main | 会话头 + 消息流 + 底部 `.workspace-input-bar` |
| 会话头 | 单聊不展示「单聊」副标题；群聊保留人数；群成员经会话头「成员」按钮打开模态 |
| 气泡布局 | 读 `userPreference.chatBubbleLayout`：`split` 己方右对方左；`left` 全部左对齐 |
| 数据 | `useXxxStore`（`-web` 阶段可在 store 内用临时数据；integrate 后只走 API） |

## 通讯录页 `/app/contacts`

| 项 | 规范 |
|----|------|
| Shell | 不开启 `show-aside`（无「活跃状态」右栏） |
| Panel | 部门树 |
| Main | 成员列表；列表项可保留在线点（presence 数据层保留） |

## 数据层约定（对接 API）

```text
Page → Pinia Store → api/admin|app/* → axios request.ts
```

**禁止** 页面直接 `import mocks/`。仓库不保留常驻 `web/src/mocks/`；勿再实现已废弃的 `isApiUnavailable` 全局 Mock 回退。

多账号切换（`stores/accountDock.ts`）会在 `localStorage` 缓存多个 JWT，属工作台产品能力，不是通用「前端风格」模板。

示范：

| 模块 | Store | API |
|------|-------|-----|
| 租户名（管理端 navbar） | `stores/tenant.ts` | `api/admin/tenant.ts` |
| 用户列表 | `stores/user.ts` | `api/admin/user.ts` |

## 与管理端边界

| 项 | 工作台 `/app/*` | 管理端 `/admin/*` |
|----|-----------------|-------------------|
| CSS | `workspace.css` | `main.css` |
| 布局 | 卡片多列 | `UDashboardSidebar` |
| API 前缀 | `/app-api/`（后续） | `/admin-api/` |

## 参考

- [workspace-ui-tokens.md](workspace-ui-tokens.md)
- [admin-ui-patterns.md](admin-ui-patterns.md) — 管理端（对照，勿混用）
