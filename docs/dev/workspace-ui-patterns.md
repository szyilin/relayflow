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
| 触发 | Rail 点击当前用户头像 → `WorkspaceProfileCard`（**账号菜单**，非飞书个人名片） |
| 头部 | 头像左、昵称+企业名右（同一水平轴）；可编辑昵称、更换头像 |
| 菜单顺序 | 我的个人名片、登录更多账号、设置、退出登录；管理后台（`isAdmin`）置底 |
| 我的个人名片 | 在账号菜单 popover 内切换到 `WorkspaceBusinessCard`（`mode=self`，可返回） |
| 个性签名 | **不在**账号菜单；编辑入口在个人名片内 |
| 登录更多账号 | `WorkspaceMoreAccountsPanel`：分组「我的企业」+「本机已登录账号」；底部「加入企业」占位、「创建新账号」→ `/app/register?addAccount=1` |
| 设置 | 关闭名片后打开独立设置窗（见下）；不在 popover 内嵌迷你设置 |
| 认证页主题 | 登录/注册页不展示主题开关；随账号用户配置加载 |

## 个人名片（飞书式 · 通用组件）

| 项 | 规范 |
|----|------|
| 组件 | `WorkspaceBusinessCard`，`mode: self \| peer` 共用一套 UI |
| 结构 | 封面区 + 叠压头像 + 显示名/企业 + 签名或备注区 + 操作按钮 |
| self | 封面可点替换；签名可编辑；**消息**可开自聊备忘；无语音/视频；无「给自己备注」 |
| peer | 签名只读；备注与描述可编辑（查看者私有）；消息接通；语音/视频占位 |
| 入口 | Rail「我的个人名片」→ self；通讯录点成员 → peer（若点自己则 self） |
| 数据 | `stores/businessCard` + `stores/profile` → app-api；契约见 [`workspace-business-card/contract.md`](../../openspec/lanes/workspace-business-card/contract.md) |

## 工作台设置窗

| 项 | 规范 |
|----|------|
| 入口 | 名片「设置」→ `WorkspaceRailHeader` 打开 `WorkspaceSettingsPanel` |
| 布局 | 左栏分类（账号与安全、通用、隐私、通知、快捷键）；右栏内容；默认「通用」 |
| 通用 | 主题模式（跟随系统/浅色/深色 + 预览）、主题色色点、会话气泡布局（左对齐/左右分布） |
| 数据 | `stores/userPreference` → **preference API 为真源**（登录/切租户后 GET 水合；PUT 失败须 toast）；localStorage 仅作带 `(tenantId, userId)` 作用域的短时缓存，**不得**作跨会话权威写路径；契约见 [`user-preference/contract.md`](../../openspec/lanes/user-preference/contract.md)；工程约定见 change [`frontend-eng-hardening-v1`](../../openspec/changes/frontend-eng-hardening-v1/proposal.md) |
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
| 点人 | `WorkspaceBusinessCard`（peer；点自己则 self）；消息 → 单聊 |

## 任务页 `/app/tasks`

| 项 | 规范 |
|----|------|
| Shell | `#panel`：个人入口「我负责的」「我创建的」「已完成」「我关注的」「动态」+ **「清单」分组**（我参与的清单、新建） |
| Main | 中栏：个人入口以列表/动态为主；**选中清单**后提供「列表 \| 看板」；根任务列表（勾选、截止、子任务进度）；**右侧详情面板** |
| 看板 | 仅清单上下文：三列 `TODO` / `IN_PROGRESS` / `DONE`；拖拽改状态与列内序；点卡片开详情。个人入口不实现完整看板（隐藏 Tab 或引导打开清单） |
| 清单 | 可选归属（`list_id` 可空）；成员 OWNER / EDITOR / VIEWER；创建任务在清单上下文默认带 `listId` |
| 详情 | 标题、负责人（可指派）、开始/截止、提醒、描述、关注人、子任务、评论、活动（复用既有面板） |
| 深链 | `?taskId=` 打开详情；`?listId=` 进入清单上下文；`?view=following\|activity` 切换个人导航 |
| 数据 | `stores/tasks` → `api/app/task`；详情与协作均无 localStorage 真源；清单/看板 integrate 后无 Mock |
| 契约 | [`workspace-tasks`](../../openspec/lanes/workspace-tasks/contract.md)、[`workspace-task-detail`](../../openspec/lanes/workspace-task-detail/contract.md)、[`workspace-task-collab`](../../openspec/lanes/workspace-task-collab/contract.md)；清单/看板见母 change [`workspace-task-list-board-v1`](../../openspec/changes/workspace-task-list-board-v1/proposal.md)（lane contract 由各 `-web` 起草） |
| 非目标（近端） | 自定义看板列、自定义字段、仪表盘、甘特 |

## 日历页 `/app/calendar`

| 项 | 规范 |
|----|------|
| Shell | `#panel`：迷你月历 +「我管理的」勾选/色/共享按钮 +「共享给我的」图层 + 添加日历 + **「我的任务」虚拟图层**（非 `cal_calendar`；可勾选） |
| Main | 顶栏（今天/翻页/日周月/创建日程）+ 自研网格；当前时间红线（日/周）；日/周可拖拽改期/拉边（组织者，**仅日程**）；任务投影与日程视觉可区分 |
| 弹层 | `CalendarEventEditor`：参与人、重复（RRULE）、编辑范围 THIS/ALL；组织者可删；**点击任务投影不打开本弹层** |
| 任务投影 | 图层开启时并行拉 `GET /app-api/task/item/due-range`；仅本人未完成（`TODO` / 落地看板后含 `IN_PROGRESS`）+ 有 `due_time`；点击 → `/app/tasks?taskId=`；**不写** `cal_event` |
| 共享 | `/app-api/calendar/share/*`；侧栏弹层增删 READ 共享 |
| 设置 | 全局设置窗「日历」分类 → `settings.calendar`（含 `showTaskLayer` 默认 true；非页内设置真源） |
| 数据 | `stores/calendar` → `api/app/calendar`；任务投影 → `api/app/task` due-range（无常驻 Mock） |
| 深链 | `?eventId=` / `?date=`；任务 → `/app/tasks?taskId=` |
| 契约 | [`workspace-calendar/contract.md`](../../openspec/lanes/workspace-calendar/contract.md)；投影增量见 [`task-calendar-projection`](../../openspec/changes/task-calendar-projection/proposal.md)（`-web` 起草 lane contract） |

## 数据层约定（对接 API）

```text
Page → Pinia Store → api/admin|app/* → axios request.ts
```

**禁止** 页面直接 `import mocks/`。仓库不保留常驻 `web/src/mocks/`；勿再实现已废弃的 `isApiUnavailable` 全局 Mock 回退。

### 模块体量（软上限）

单 SFC / 单 Pinia store 建议 **≤ ~500 行**。日历、IM、消息、任务等易膨胀模块：逻辑进 `composables/`，大块 UI 进 `components/workspace|…`；页面只做编排与深链。超过软上限须拆分或在文件头注明待拆边界。详见 [`frontend-eng-hardening-v1`](../../openspec/changes/frontend-eng-hardening-v1/design.md)。

### 企业 / 账号切换与租户状态

`tenantId` 或 `userId` 变更时（`useTenantSwitchReload` 等），MUST 清空并按需重载**全部**租户范围 store，至少包括：`profile`、`im`、`contacts`、`tasks`、`calendar`、`userPreference`。禁止切换后仍展示上一租户的任务、日程或偏好。

### 列表分页

工作台列表调用分页 API 时 MUST 使用显式分页或「加载更多」；默认 `pageSize` 建议与 [`api.md`](api.md) 一致（20），且不超过服务端上限 100。**禁止**固定拉一页 100 条且无翻页而假装列表完整。角标/计数若只有分页数据，不得静默冒充全量。

### Account Dock 与 JWT（威胁模型）

多账号切换（`stores/accountDock.ts`）属工作台产品能力：V1 可在 `localStorage`（键 `relayflow:account-dock`）持久化多条「账号×企业」会话以便一键切换，条目中 **MAY** 含 bearer `token`。

| 项 | 说明 |
|----|------|
| 风险 | 页面 XSS 可读 `localStorage`，一次窃取 **Dock 内全部 JWT**（相对「仅当前会话 1 个 token」放大） |
| V1 约束 | 登出 MUST 清除该 `userId` 下全部 dock 条目；同账号跨企业 MUST 走 `POST …/tenant/switch` 刷新 token，不得仅靠过期拷贝 token 当作切换成功 |
| 目标态 | httpOnly Cookie / 服务端 opaque session id + 会话列表（另立项；本阶段不实现） |

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
