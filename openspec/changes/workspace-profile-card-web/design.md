## Context

归档 change `workspace-profile-card` 已交付：Rail 点头像弹出资料卡、昵称/头像编辑、管理后台与退出、Account Dock 多会话。现状仍有几处与目标交互不符：

- 资料卡头部为「头像在上、昵称居中」纵向布局，非飞书式左右同轴。
- 账号切换挂在 Rail 名称旁 chevron（`WorkspaceAccountSwitcher`），与名片菜单割裂。
- 主题切换浮在 `WorkspaceShell` 右上角（`AdminColorModeToggle`）。
- 消息/通讯录 `show-aside` 展示「活跃状态」；单聊会话头副标题写死「单聊」。

本 change 为 **纯前端 `-web`**，消费已有 profile / tenant switch / account dock，不改 Java。

## Goals / Non-Goals

**Goals:**

- 资料名片成为个人入口单源：占位菜单 + 已接通能力（资料编辑、多企业/多账号、设置·主题、退出、管理后台）。
- 「登录更多账号」二级面板对齐参考图：列出可切换企业/账号，底部「加入企业」「创建新账号」显式占位。
- 精简会话头与右栏噪音；主题入口迁入设置。

**Non-Goals:**

- 签名/名片/加入企业后端与持久化。
- 删除 presence API；不在会话列表/头像上重做在线点（可后续切片）。
- 管理端用户菜单改造。

## Decisions

### D1. 组件拆分

| 组件 | 职责 |
|------|------|
| `WorkspaceProfileCard` | 名片主面板：头像+昵称同轴、租户名、菜单项 |
| `WorkspaceSettingsPanel`（新） | 设置子面板/二级 popover：主题切换 + 其余占位 |
| `WorkspaceMoreAccountsPanel`（新或重构自 AccountSwitcher） | 「登录更多账号」模态/面板 |

Rail 去掉独立 chevron；`WorkspaceAccountSwitcher` 逻辑迁入 MoreAccounts，避免双入口。

**备选**：全部塞进一个巨型 ProfileCard → 否决，难以迭代占位。

### D2. 「登录更多账号」数据源

面板同时展示两类可切换目标（去重）：

1. **同身份多企业**：`authStore.tenants`（`GET /app-api/system/tenant/my-list`），非当前 `tenantId` 可 `switchTenant`。
2. **本机多会话**：`accountDock` 中其他 `userId:tenantId`，走 `switchToDockEntry`。

副文案可用「已绑定企业」+ 当前用户展示名/手机号（有则显示）。无其他项时仍展示空列表 + 底部占位操作。

「创建新账号」：链到 `/app/register?addAccount=1`（已有）或纯占位 toast——默认 **链到注册页**，文案仍标为可扩展入口。「加入企业」：**显式占位**（disabled 或 toast「即将推出」+ `（占位）` 标签）。

**备选**：只展示 dock → 否决，与参考图「同账号多企业」不符。

### D3. 占位符可见约定

所有未接通项 MUST 带统一视觉标记，便于后续替换：

- 文案后缀或旁注 `（占位）`，或右侧 `即将推出` muted 标签。
- 点击：toast「功能即将推出」或 no-op；禁止静默无反馈。

### D4. 设置与主题

- 名片点「设置」→ 打开 `WorkspaceSettingsPanel`（二级 UPopover / 小型 UModal 均可，优先二级面板以免打断工作台）。
- 主题：复用 `useAdminColorMode`（或抽成 `useWorkspaceColorMode` 别名，实现仍共享）；从 `WorkspaceShell` **移除**右上角浮层。
- 登录页 `workspace-auth` / `auth` layout 上的主题开关可保留（未登录无名片）。

### D5. 去掉「单聊」与「活跃状态」

- `conversationSubtitle`：`direct` 返回空字符串或省略副标题节点；群聊 / `bot_dm` 不变。
- 消息页、通讯录：`show-aside` 关闭或不再传 `#aside`；群成员列表若仅存在于 aside，**群聊成员改到主区可点入口或会话头菜单**——若当前仅 aside 有群成员，须把群成员面板迁到会话头「群信息」入口或主区抽屉，避免功能回退。

**决策**：群成员列表迁到会话头旁按钮打开的抽屉/面板（轻量）；单聊不再显示旁栏。通讯录页直接去掉 aside。

### D6. 管理后台入口

保持 `authStore.isAdmin` 才显示；位置固定名片最底部（分隔线以下），外链图标保留。

## Risks / Trade-offs

- [群成员入口回退] → 迁移前先盘点消息页 aside 群成员 UI，tasks 含「迁入口」验收。
- [租户列表与 dock 认知混淆] → 面板内分组标题：「我的企业」「本机已登录账号」。
- [主题仅工作台壳层移除] → 登录页仍可切主题，避免未登录无法切暗色。

## Migration Plan

1. 前端按 tasks 分批改 UI → `pnpm build && pnpm typecheck`。
2. 更新 `docs/dev/workspace-ui-patterns.md`（右栏、主题入口）。
3. 无 Flyway / 无后端发布依赖；回滚即 revert 前端提交。

## Open Questions

- 「创建新账号」最终用注册页还是纯占位：默认链注册；实现时可按产品偏好改。
- 群成员抽屉交互细节：实现时对齐现有 `ImInviteMembersModal` 风格即可，不单独立项 API。
