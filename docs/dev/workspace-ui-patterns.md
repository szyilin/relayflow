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
| 左导航 | 固定宽卡片：品牌、搜索占位、图标+文字菜单、底部用户 |
| 列表栏 | `#panel` slot；右缘可拖拽调宽 |
| 主区 | 默认 slot；聊天/任务/文档内容 |
| 右栏 | 可选 `#aside`（如消息页「活跃状态」） |
| 主题 | 右上角 `AdminColorModeToggle` |

## 认证页 `/app/login`

| 项 | 规范 |
|----|------|
| Layout | `meta.layout: workspace-auth` |
| 表单 | 居中 `UCard`；提交走 `useAuthStore().login()` |
| 跳转 | 成功 → `/app/messages` |

## 消息页 `/app/messages`

| 项 | 规范 |
|----|------|
| Shell | `show-aside` 开启右栏 |
| Panel | 会话列表 + 搜索 + `.workspace-list-item` |
| Main | 会话头 + 占位内容 + 底部 `.workspace-input-bar` |
| 数据 | `useXxxStore`（原型阶段 store 内 Mock 回退） |

## 数据层约定（对接 API）

```text
Page → Pinia Store → api/admin|app/* → axios request.ts
                      ↘ mocks/*（仅 store 内 isApiUnavailable 回退）
```

**禁止** 页面直接 `import mocks/`。

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
