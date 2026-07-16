## Why

工作台个人区仍偏「脚手架拼接」：会话头暴露「单聊」、右侧「活跃状态」栏占用空间且主题开关浮在壳层右上角，资料卡片菜单过少、与飞书式账号切换入口脱节。需要一次以 UI 为主的切片，把个人入口收成可扩展的名片菜单，并复用已有多租户/多账号能力。

## What Changes

- **资料名片重排**：点击 Rail 头像弹出的卡片改为头像与昵称同水平轴；保留已有昵称编辑、头像上传、退出登录、管理后台入口。
- **菜单结构对齐参考图**：个性签名、我的个人名片为**显式占位**；设置、退出登录、管理后台（置底）；「登录更多账号」可点并打开二级面板。
- **登录更多账号（对接已有能力）**：二级面板列出当前身份已绑定的其他企业（`my-list` / 切换租户）及本机已登录的其他账号会话（Account Dock）；下方「加入企业」「创建新账号」为显式占位（创建可择一链到现有注册页或纯占位，tasks 中定）。
- **设置入口迁入名片**：主题切换从 `WorkspaceShell` 右上角浮层移入「设置」；本期设置页仅主题 + 其余占位。
- **去掉会话头「单聊」标签**：单聊会话头副标题不再展示「单聊」（群聊人数、助手等保留）。
- **去掉「活跃状态」右栏**：消息页 / 通讯录页不再展示该 aside；presence 能力保留在数据层，不在本 change 删除后端。
- **Rail 触发区**：去掉名称旁独立的账号切换 chevron（能力并入名片内「登录更多账号」）；Rail 头像与名称保持同水平轴、垂直居中。

**非目标（本 change 不做）**

- 个性签名 / 个人名片 / 加入企业的真实 API 与持久化
- 企业认证流程、状态（+ 状态）按钮
- 删除 presence API / WS
- 管理端 `AdminUserMenu` 改造

## Capabilities

### New Capabilities

- `workspace-profile-menu`: 工作台资料名片菜单、设置（含主题）、登录更多账号二级面板、会话头/右栏精简的产品行为

### Modified Capabilities

- （无）现有 `system` 资料/租户 API 与 `web-auth` 登录行为不改契约；本切片只消费已有能力。

## Impact

- **前端 `web/`**：`WorkspaceProfileCard`、`WorkspaceRailHeader`、`WorkspaceAccountSwitcher`（或内联重构）、`WorkspaceShell`、消息页/通讯录右栏、可选新建 Settings / MoreAccounts 组件；文档 `docs/dev/workspace-ui-patterns.md` 同步右栏与主题入口说明。
- **后端 / Maven**：不改；复用 `GET /app-api/system/user/profile`、`GET /app-api/system/tenant/my-list`、`POST /app-api/system/tenant/switch`、Account Dock 本地会话。
- **回滚**：纯前端；回退本 change 前端提交即可，无 DB 迁移。
