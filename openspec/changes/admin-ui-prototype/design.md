# 设计：管理端 UI 原型（Mock 全壳层）

## Context

- **视觉基调**：已选 `admin-ui-design-direction` → **B · Clean Enterprise**，主题跟随系统，登录左右分栏，启用 design-preview
- **现状**：`layouts/admin.vue` 仅 1 个导航项；模板 demo 页仍在 `pages/` 根目录
- **目标**：一眼看到 RelayFlow 管理端 **整体框架**，内容可为占位，数据全部 Mock

## Goals / Non-Goals

**Goals:**

1. 可浏览的 **完整 IA**（信息架构）与 sidebar 分组
2. 代表型页面各 1 个：**登录、概览、列表、表单、空状态、组件板**
3. Mock 登录/退出/守卫，**零后端依赖**
4. 为后续切片 **保留目录与组件结构**（换 API 时不重写页面骨架）

**Non-Goals:**

- 真实 `POST /admin-api/system/auth/login`
- RBAC 动态菜单、WebSocket、IM 聊天 UI
- 用户端 `/app/*`
- 像素级 Figma 稿

---

## 信息架构（V1 占位）

```text
/admin/login                    # auth layout，Mock 登录
/admin                          # 概览（统计卡片 + 快捷入口）
/admin/design-preview           # 组件/token 板（不进菜单，直链访问）

/admin/system/user              # 用户列表（Mock 表格 + 分页）
/admin/system/user/create       # 用户表单占位（分组字段 + 提交 toast）
/admin/system/dept              # 部门树占位（UEmpty 或骨架树）

/admin/infra/file               # 文件列表占位（表格 + 空操作）
```

Sidebar 分组：

| 分组 | 菜单项 | 路由 |
|------|--------|------|
| （顶） | 概览 | `/admin` |
| 系统管理 | 用户管理 | `/admin/system/user` |
| 系统管理 | 部门管理 | `/admin/system/dept` |
| 基础设施 | 文件管理 | `/admin/infra/file` |
| （底） | 设计预览 · 仅 dev | `/admin/design-preview`（可选 footer link） |

IM / 工作流模块：**V1 原型不出现**，避免空菜单膨胀。

---

## 目录结构（web/src）

```text
web/src/
├── assets/css/main.css          # B 方向 token（与 design-direction 一致）
├── layouts/
│   ├── admin.vue                # 壳层：sidebar + navbar + RouterView
│   └── auth.vue                 # 登录：左右分栏，无 sidebar
├── pages/admin/
│   ├── login.vue
│   ├── index.vue                # 概览
│   ├── design-preview.vue
│   ├── system/user/index.vue
│   ├── system/user/create.vue
│   ├── system/dept/index.vue
│   └── infra/file/index.vue
├── mocks/
│   ├── auth.ts                  # mockLogin / mockLogout / mockUser
│   ├── tenant.ts                # 默认租户名
│   ├── dashboard.ts             # 统计卡片数据
│   └── system/users.ts          # 用户列表 + 分页
├── stores/
│   ├── auth.ts                  # Pinia：token、user、login、logout（读 mocks）
│   └── tenant.ts                # Pinia：tenantName（读 mocks）
├── components/admin/
│   ├── AdminNavbar.vue          # 标题、breadcrumb、租户、UserMenu
│   ├── AdminUserMenu.vue        # 头像、退出
│   └── AdminPageHeader.vue      # 列表/表单页统一标题区
├── composables/
│   └── useAdminNav.ts           # sidebar items 单源
└── router/
    └── guards.ts                # beforeEach：Mock 鉴权（或 inline main.ts）
```

**原则**：页面 **不** `fetch('/admin-api/...')`；数据来自 `stores` 或 `mocks` import。

---

## Mock 行为

### 登录

- 任意非空用户名 + 密码 → `mockLogin()` 写入 `localStorage`（key：`relayflow:admin:mock-token`）
- 固定 Mock 用户：`admin` / 昵称「管理员」
- 登录成功 → `/admin`；失败 → toast（仅空字段校验）

### 路由守卫

| 条件 | 行为 |
|------|------|
| 无 token，访问 `/admin/*`（非 login） | → `/admin/login?redirect=...` |
| 有 token，访问 `/admin/login` | → `/admin` |
| `/admin/login` | 使用 `auth` layout |

### 退出

- UserMenu → logout → 清 token → `/admin/login`

### Mock 数据示例

```ts
// mocks/system/users.ts — 概念
export const mockUsers = [
  { id: '1', username: 'admin', nickname: '管理员', dept: '总部', status: '启用', createTime: '2026-01-01' },
  { id: '2', username: 'zhangsan', nickname: '张三', dept: '研发部', status: '启用', createTime: '2026-02-15' },
  // … 8～10 条供分页演示
]
```

---

## 页面模式（占位内容）

### 1. 登录 `/admin/login`（auth layout）

- **左**：teal 渐变 + Logo + 「RelayFlow」「企业协作平台 · 管理控制台」
- **右**：`UAuthForm` 或 `UCard` + 用户名/密码；提交走 Mock store
- 底部小字：「原型模式 · 任意账号可登录」

### 2. 概览 `/admin`

- 4 个 `UCard` 统计（Mock：用户数、在线、存储、消息 — 假数字）
- 下方「快捷入口」按钮链到 user/file 页

### 3. 用户列表 `/admin/system/user`

- `AdminPageHeader` + 「新建用户」→ create
- 筛选栏（`UInput` 占位，本地 filter Mock 数据）
- `UTable` + 分页（Mock 切页）
- 行操作：编辑/禁用（toast「原型未实现」）

### 4. 用户表单 `/admin/system/user/create`

- 分组：`UCard`（基本信息、归属部门）
- 字段占位 + 底部「保存」→ toast 成功 → 回列表

### 5. 部门 `/admin/system/dept`

- `UEmpty` + 文案「部门管理即将上线」或简单 `UTree` Mock 3 节点

### 6. 文件 `/admin/infra/file`

- 表格列占位 + 上传按钮 disabled + tooltip「接 API 后启用」

### 7. design-preview `/admin/design-preview`

- 色板、Button 变体、Alert、Form、Table、Empty、Skeleton 各一块

---

## 与模板 demo 的处理

| 文件 | 处理 |
|------|------|
| `pages/customers.vue` 等 | **删除** 或移至 `pages/_deprecated/`（不参与 auto-routes） |
| `pages/index.vue` | 重定向到 `/admin` 或保留极简 landing 链到 admin |
| `layouts/default.vue` | 若仅 demo 使用，可删或保留给未来用户端 |

---

## 技术决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 状态 | Pinia | 项目约定；与后续真 auth 一致 |
| Mock 层 | `mocks/` + store 封装 | 换 API 时只改 store，页面不动 |
| 路由 | 继续 file-based auto routes | 与现脚手架一致 |
| API 层 | **本 change 不建** `api/admin/*` | 避免误接后端；下阶段 login-slice 再建 |
| Pinia | 若 `package.json` 无 pinia 则添加 | 轻量、标准 |

---

## 验收标准（浏览器）

```text
pnpm dev
1. /admin/login — 分栏登录，任意账号进入
2. /admin — 统计卡片 + 快捷入口
3. Sidebar 切换 user / dept / file — 无 404，壳层一致
4. 用户列表筛选、分页、跳转 create — 交互流畅（Mock）
5. 退出 → 回登录；直接访问 /admin → 重定向登录
6. /admin/design-preview — 组件展示
7. pnpm build 通过
```

---

## 后续衔接

| 阶段 | Change | 工作 |
|------|--------|------|
| 当前 | `admin-ui-prototype` | Mock 全壳层，视觉验收 |
| 下一 | `admin-login-slice` | `api/admin/auth.ts` + 真 JWT，**替换** store 登录逻辑，页面保留 |
| 再后 | `admin-shell-slice` | 租户 API 替换 mock tenant |
| 再后 | `admin-user-list-slice` | 用户 API 替换 mock users |

---

## Risks

| 风险 | 缓解 |
|------|------|
| Mock 与真 API 字段不一致 | mocks 字段名对齐 `docs/dev/api.md` 与现有 VO 命名 |
| 原型代码堆积 | store 层抽象；login-slice 任务含「移除 mock-token 路径」 |
| 偏离纵向切片 | 文档标明 **一次性 UI 先行**；验收后立即接 login-slice |
