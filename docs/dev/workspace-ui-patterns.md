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
| 左导航 | 固定宽卡片：品牌、**可点搜索**、图标+文字菜单；顶部 `WorkspaceRailHeader`（头像+昵称同轴 + 头像旁「+」Create Hub） |
| 列表栏 | `#panel` slot；右缘可拖拽调宽 |
| 主区 | 默认 slot；聊天/任务/文档内容 |
| 右栏 | 可选 `#aside`（默认关闭；消息/通讯录不展示「活跃状态」） |
| 主题 | **不**浮在壳层右上角；从资料名片 → **设置窗**切换 |
| 设置窗 | `WorkspaceSettingsPanel`：宽 UModal；左栏分类 + 右栏「通用」 |
| 弹窗稳定 | 内容增减时外壳勿上下跳：`WorkspaceModalBody`（`max-h` + 内滚 + `min-h`）；操作按钮优先放 `UModal` `#footer`；切换态（loading/empty/列表）共用相近 min-height |

## Create Hub（「+」创建）

| 项 | 规范 |
|----|------|
| 触发 | `WorkspaceRailHeader` 头像行右侧「+」→ `UDropdownMenu` |
| 菜单项 | 创建群组、添加企业成员、添加外部联系人、创建文档（本期仅此四项） |
| 添加企业成员 | 仅 `canInvite` 时显示；真源 `GET /app-api/system/org-invite/capability` |
| 创建群组 | `WorkspaceCreateGroupModal` + `WorkspaceMemberPicker`（组织内 / 外部 / 我管理的群组） |
| 选人器 | `WorkspaceMemberPicker`：三源；`excludeUserIds` 隐藏已选/已在群；「我管理的群组」=`stores/memberPicker`（IM 会话+members 聚合，非 seed） |
| 邀请入群 | `ImInviteMembersModal` **必须**内嵌同一 picker，并传入当前群 `excludeUserIds` |
| 添加企业成员 UI | `WorkspaceOrgInviteModal` 四 Tab（QR/链接/码/手机号）；`stores/orgInvite` → `/app-api/system/org-invite/*`；加入页 `/app/invite/join` |
| 添加外部联系人 | `WorkspaceAddExternalContactModal`；`contactAdd` → `search-account` / `POST request` |
| 创建文档 | 跳转 `/app/docs` + `docs.createPage()` |
| 状态 | `stores/createHub.ts`（`canInvite` 读 orgInvite capability）；契约 [`workspace-create-hub`](../../openspec/lanes/workspace-create-hub/contract.md) · [`org-invite`](../../openspec/lanes/org-invite/contract.md) · [`im-group-chat` managed](../../openspec/lanes/im-group-chat/contract.md) |

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
| peer | 签名只读；备注与描述可编辑（查看者私有）；消息接通；语音/视频占位；封面右上角 **⋯** 菜单可「设为/取消星标联系人」 |
| 入口 | Rail「我的个人名片」→ self；通讯录点成员 → peer（若点自己则 self）；**列表所有者芯片** → self/peer |
| 数据 | `stores/businessCard` + `stores/profile` → app-api；契约见 [`workspace-business-card/contract.md`](../../openspec/lanes/workspace-business-card/contract.md)；星标走 `stores/contacts` → `/app-api/system/contact/starred/*` |

## 所有者芯片（列表通用）

| 项 | 规范 |
|----|------|
| 组件 | `WorkspaceOwnerChip` |
| 展示 | 头像（图或 **哈希渐变字母底**）+ **真实显示名**；当前用户可旁标「我」，**禁止**单独用「我」代替姓名 |
| 点击 | `stopPropagation` + `useClickAnchoredPopover` → `WorkspaceBusinessCard`；不得单独触发行打开 |
| 数据 | `userId` 必填；`displayName` / `avatarUrl` 可选；否则从 auth / contacts 解析 |
| 头像 | 统一 `WorkspaceAvatar`（seed=`userId`）；无 URL 时渐变字母，禁止中性灰圆 |
| 复用 | 云文档列表所有者列、任务负责人等；多负责人最多约 3 个 chip + `+N` |
| Change | [`workspace-owner-chip-web`](../../openspec/changes/archive/2026-07-27-workspace-owner-chip-web/proposal.md) |

## 视觉系统（身份色 / 状态色 / 空态）

| 项 | 规范 |
|----|------|
| 边界 | 色用在人/态/空；画布、列卡片、正文、多数工具图标保持冷静（见 `workspace-ui-tokens.md`） |
| 字母头像 | `WorkspaceAvatar` + `avatarGradientFromId`；IM/通讯录/名片/Rail 等同规则；有头像时传 `src`（fileId 亦可，组件内 `resolveAvatarUrl`） |
| 会话/联系人头像字段 | API 须返回 `avatar`（fileId）；前端禁止只渲染 `avatarText` 而忽略已有图 |
| 语义标签 | `WorkspaceSemanticBadge`：`kind` = official/bot/agent/external/role/comingSoon/info/success/warning/danger |
| 「即将推出」 | MUST 用 `comingSoon` soft 标签，禁止 muted 纯文字冒充 |
| Rail 模块 tint | `useWorkspaceNav` 的 `tintVar` → `--ws-module-*`；未选中可辨、选中靠 active 底 |
| 空态 | `WorkspaceEmptyState`（`compact` 小图标 / `hero` 大插画）；设置占位、列表空、主区无选等优先接入 |
| 插画资源 | **Storyset** → 官网调色/裁层后落 `web/src/assets/illustrations/`；登记 `workspaceIllustrations.ts`；约定见 [`workspace-illustrations.md`](workspace-illustrations.md) |
| 消息未选会话 | 主区 `hero` + idle 插画池随机一张；**禁止**自动选中首条会话（已读） |
| Change | [`workspace-visual-polish-web`](../../openspec/changes/workspace-visual-polish-web/proposal.md) |

## 工作台设置窗

| 项 | 规范 |
|----|------|
| 入口 | 名片「设置」→ `WorkspaceRailHeader` 打开 `WorkspaceSettingsPanel` |
| 布局 | 左栏分类（账号与安全、通用、隐私、通知、快捷键）；右栏内容；默认「通用」 |
| 通用 | 主题模式（跟随系统/浅色/深色 + 预览）、主题色色点、会话气泡布局（左对齐/左右分布） |
| 数据 | `stores/userPreference` → **preference API 为真源**（登录/切租户后 GET 水合；PUT 失败须 toast）；localStorage 仅作带 `(tenantId, userId)` 作用域的短时缓存，**不得**作跨会话权威写路径；契约见 [`user-preference/contract.md`](../../openspec/lanes/user-preference/contract.md)；工程约定见 change [`frontend-eng-hardening-v1`](../../openspec/changes/frontend-eng-hardening-v1/proposal.md) |
| 占位分类 | `WorkspaceEmptyState` + `WorkspaceSemanticBadge`（comingSoon）+ toast |

## 全局搜索（⌘K / Rail）

| 项 | 规范 |
|----|------|
| 入口 | Rail 搜索输入点击；工作台页 `⌘K` / `Ctrl+K`（`useWorkspaceSearchShortcut`） |
| UI | `WorkspaceSearchModal`：关键词输入 + `member` / `conversation` / `task` 分组结果 |
| 数据 | `stores/workspaceSearch` → `GET /app-api/infra/workspace-search`（无 Mock） |
| 深链 | `/app/contacts?memberId=`、`/app/messages?conversationId=`、`/app/tasks?taskId=`；目标页读 query 激活上下文 |
| 契约 | [`openspec/lanes/workspace-search/contract.md`](../../openspec/lanes/workspace-search/contract.md) |

## 认证页 `/app/login` · `/app/register`

| 项 | 规范 |
|----|------|
| Layout | `meta.layout: workspace-auth`（居中表单；背景见 `workspace.css` `.workspace-auth-*`） |
| 视觉 | 居中 `UCard` 承载表单；**卡片内控件须撑满**（全局主题已默认 `w-full`，见 `web/vite.config.ts`） |
| 登录 | 手机号 + 密码；提交走 `useAuthStore().login()`；多企业时同页选租户 |
| 注册 | 手机号 / 昵称 / 企业名 / 密码；**不展示**「关于企业邀请」说明块 |
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
| Panel | 飞书式**分类导航**（图标）：组织内联系人、外部联系人、新的联系人、星标联系人、我的群组；**不做**邮箱联系人 |
| 组织内 | 主区：部门面包屑 + 子部门行（「下级」）+ 直属成员；数据仍走 `dept/tree` · `list-by-dept`；页内不设搜索（统一走 Rail ⌘K） |
| 外部 | 按显示名首字母分组；顶栏「添加」打开 `WorkspaceAddExternalContactModal`（与 Create Hub 共用）；数据 `GET …/external/list` |
| 新的联系人 | 好友申请列表；「同意」/`accept`、「拒绝」/`reject` API |
| 添加外部流 | 确切手机号/邮箱搜索 → `POST request`；契约见 enrich「发起添加」章 |
| 星标 | 星标列表 + 取消星标（`GET/PUT …/contact/starred/*`）；**设为星标**入口在 peer 名片封面右上角「⋯」菜单 |
| 我的群组 | IM 会话 `type=group`；点击进 `/app/messages`；无临时群占位 |
| 点人 | `WorkspaceBusinessCard`（peer；点自己则 self）；消息 → 单聊 |
| 组件 | `components/workspace/contacts/*` |

## 任务页 `/app/tasks`

| 项 | 规范 |
|----|------|
| Shell / 左栏 | **三类**：① 快速访问（查询预设，非清单）：「我负责的」「我创建的」「我分配的」「已完成」「我关注的」「全部任务」「动态」；② **「清单」分组**（我参与的清单、新建）；③ 各上下文工具栏读/写 ViewConfig |
| 快速访问种子 | `MINE`=负责人含我；`CREATED`=我创建；`ASSIGNED_BY_ME`=分配人是我且我非负责人；`COMPLETED`=已完成；`FOLLOWING`=我关注；`ALL`=合同可见并集（非租户全量） |
| Main | 列表 \| 看板由 ViewConfig `displayMode` 驱动；同一上下文下 `groupBy`/筛选/排序在两种展示间共享；根任务 + **右侧详情面板** |
| 三套分组 | ① 字段 `groupBy`（系统字段 + 清单自定义单选）；空值进「无分组」；拖拽写字段。②「我负责的」`PERSONAL_CUSTOM` 个人组（私有，删组回默认组）。③ 清单无字段分组时用清单内组（默认组 + 自定义组） |
| 清单 | 多清单归属（`task_list_item`）；成员 OWNER / EDITOR / VIEWER；创建任务在清单上下文默认加入该清单 |
| 详情 | 标题、**多负责人**、分配人、开始/截止、提醒、描述、关注人、子任务、评论、活动、**多清单加入/移出**、清单自定义字段取值 |
| 深链 | `?taskId=` 打开详情；`?listId=` 进入清单；`?view=` 切换快速访问上下文 |
| 数据 | `stores/tasks` → `api/app/task`；无 localStorage / Mock 真源 |
| 契约 | [`workspace-tasks`](../../openspec/lanes/workspace-tasks/contract.md)、[`workspace-task-detail`](../../openspec/lanes/workspace-task-detail/contract.md)、[`workspace-task-collab`](../../openspec/lanes/workspace-task-collab/contract.md)、[`workspace-task-list`](../../openspec/lanes/workspace-task-list/contract.md)、[`workspace-task-quick-views`](../../openspec/lanes/workspace-task-quick-views/contract.md)、[`workspace-task-view-config`](../../openspec/lanes/workspace-task-view-config/contract.md)、[`workspace-task-group-by-field`](../../openspec/lanes/workspace-task-group-by-field/contract.md)、[`workspace-task-multi-assignee`](../../openspec/lanes/workspace-task-multi-assignee/contract.md)、[`workspace-task-assigner`](../../openspec/lanes/workspace-task-assigner/contract.md)、[`workspace-task-mine-groups`](../../openspec/lanes/workspace-task-mine-groups/contract.md)、[`workspace-task-multi-list`](../../openspec/lanes/workspace-task-multi-list/contract.md)、[`workspace-task-list-groups`](../../openspec/lanes/workspace-task-list-groups/contract.md)、[`workspace-task-custom-field`](../../openspec/lanes/workspace-task-custom-field/contract.md) |
| 非目标（近端） | 仪表盘、甘特、跨清单共享自定义字段 schema |

## 日历页 `/app/calendar`

| 项 | 规范 |
|----|------|
| Shell | `#panel`：迷你月历 +「我管理的」勾选/有效色 +「我订阅的」（READ 共享 + `ALL_STAFF`）+ 添加日历（受组织策略门禁）+ **「我的任务」虚拟图层**（非 `cal_calendar`；可勾选） |
| 行菜单 | 每行 `···`：仅显示此日历 / 分享（所有者）/ 修改颜色（**本机色**）/ 日历设置 / 删除（OWNED） |
| 日历设置 | `CalendarSettingsModal`：所有者改名称·描述·默认色并管理共享；订阅者名称/描述只读，改本机色；可恢复默认色 |
| Main | 顶栏（今天/翻页/日周月/创建日程）+ 自研网格；当前时间红线（日/周）；日/周可拖拽改期/拉边（组织者，**仅日程**）；任务投影与日程视觉可区分 |
| 弹层 | `CalendarEventEditor`：参与人、重复（RRULE）、编辑范围 THIS/ALL；组织者可删；**点击任务投影不打开本弹层** |
| 任务投影 | 图层开启时并行拉 `GET /app-api/task/item/due-range`；仅本人未完成（`TODO`）+ 有 `due_time`；点击 → `/app/tasks?taskId=`；**不写** `cal_event` |
| 共享 | `/app-api/calendar/share/*`；`···` 分享或设置内「管理共享」弹层增删 READ |
| 全员日历 | 管理端 `/admin/products/calendar/all-staff` 创建；scope 内成员 App list 自动出现在「我订阅的」；元数据仅 Admin 改 |
| 本机色 | `PUT /app-api/calendar/calendar/display-color`；list 含 `defaultColor` / `displayColor` / `effectiveColor`；事件 `calendarColor` 为有效色 |
| 组织策略 | `GET /app-api/calendar/calendar/org-policy`；`allowMemberCreateOwnedCalendar=false` 时隐藏「添加日历」（写路径仍后端强制） |
| 设置 | 全局设置窗「日历」分类 → `settings.calendar`（含 `showTaskLayer` 默认 true；非页内设置真源；**不含**按日历本机色） |
| 数据 | `stores/calendar` → `api/app/calendar`；任务投影 → `api/app/task` due-range（无常驻 Mock） |
| 深链 | `?eventId=` / `?date=`；任务 → `/app/tasks?taskId=` |
| 契约 | [`workspace-calendar/contract.md`](../../openspec/lanes/workspace-calendar/contract.md)；Admin 见 [`admin-calendar-product-settings/contract.md`](../../openspec/lanes/admin-calendar-product-settings/contract.md)；投影增量见 [`task-calendar-projection`](../../openspec/changes/task-calendar-projection/proposal.md) |

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
