# 管理端 UI 页面模式

> **来源**：摘自 `admin-ui-prototype` 验收版，对照 `web/src/pages/admin/` 与 `components/admin/`。  
> 接 API 时 **只换 store / api 层**，template 与布局遵循本文档。

## 目录约定

```text
web/src/
├── layouts/admin.vue          # 壳层（sidebar + RouterView）
├── layouts/auth.vue           # 登录（无 sidebar）
├── components/admin/          # 壳层复用组件
│   ├── AdminNavbar.vue
│   ├── AdminUserMenu.vue
│   └── AdminPageHeader.vue
├── composables/useAdminNav.ts # sidebar 导航单源
├── pages/admin/               # 管理端页面
├── stores/                    # Pinia
└── api/admin/                 # axios 封装下的管理端 API
```

## 壳层（`/admin/*`，登录除外）

**文件**：`layouts/admin.vue` + `composables/useAdminNav.ts`

| 区域 | 实现 |
|------|------|
| Sidebar | `UDashboardGroup` + `UDashboardSidebar`（可折叠、可拖拽） |
| 品牌 | workflow 图标 + RelayFlow |
| 导航分组 | 概览 → 系统管理 → 基础设施；**仅 DEV** 底栏「设计预览」 |
| Active 项 | 左 2px primary + 浅 primary 背景 |
| 用户区 | Sidebar footer `AdminUserMenu` |
| 内容 | 各页 `UDashboardPanel` + `AdminNavbar` |

**Navbar**（`AdminNavbar.vue`）：左侧 `UDashboardSidebarCollapse` + 页面标题；右侧租户名 + `AdminUserMenu`。

## 认证

**唯一登录页**：`/app/login`（`/admin/login` 重定向至此）。成功后默认进入工作台 `/app/messages`，管理后台入口在工作台内按 `isAdmin` 展示。

## 概览页 `/admin`

**文件**：`pages/admin/index.vue`

| 项 | 规范 |
|----|------|
| 结构 | `UDashboardPanel` → `AdminNavbar` → body |
| 内容 | 当前为占位 `UEmpty`；后续再补统计/快捷入口 |

## 列表页 `/admin/system/user`

**文件**：`pages/admin/system/user/index.vue`

| 项 | 规范 |
|----|------|
| 标题区 | `AdminPageHeader` + 右侧主操作（如「新建用户」`UButton` `to=`） |
| 筛选 | `UInput` + 搜索按钮，位于 `UCard` 内表格上方 |
| 表格 | `UTable` + `@tanstack/table-core` 列定义 |
| 分页 | `UPagination`，底部展示总条数 |
| 行操作 | 次要按钮 + toast（危险操作用 `color="error" variant="soft"`） |

## 表单页 `/admin/system/user/create`

**文件**：`pages/admin/system/user/create.vue`

| 项 | 规范 |
|----|------|
| 宽度 | `max-w-2xl mx-auto` |
| 分组 | 多个 `UCard`（基本信息、归属部门等） |
| 字段 | `UFormField` + `UInput` / `USelectMenu` |
| 操作 | 底部「保存」primary + 「取消」ghost `to` 回列表 |
| 反馈 | `useToast()` 成功/失败 |

## 空状态 / 树占位 `/admin/system/dept`

**文件**：`pages/admin/system/dept/index.vue`

| 项 | 规范 |
|----|------|
| 无数据 | `UEmpty` + 主操作引导 |
| 树形 | `UTree` + Mock 节点（接 API 后换数据） |

## 表格占位 `/admin/infra/file`

**文件**：`pages/admin/infra/file/index.vue`

| 项 | 规范 |
|----|------|
| 上传 | 主操作放 `AdminPageHeader` actions；未接 API 时 `disabled` + `UTooltip` |
| 表格 | `UTable` 列占位 |

## 设计预览 `/admin/design-preview`

**文件**：`pages/admin/design-preview.vue`

- 展示 token 色板与常用组件组合
- **仅 `import.meta.env.DEV`** 时出现在 sidebar 底栏；生产构建不展示

## 接 API

| 层 | 约定 |
|----|------|
| 数据 | `web/src/api/admin/*.ts` + Pinia store |
| 登录 token | `web/src/utils/session-storage.ts`（`relayflow:access-token` 等） |
| 页面 template | 接 API 时尽量保留布局，只改 script 数据来源 |

## 参考

- [admin-ui-tokens.md](admin-ui-tokens.md)
- [admin-ui-workflow.md](admin-ui-workflow.md)
- `.cursor/rules/admin-ui-patterns.mdc`
