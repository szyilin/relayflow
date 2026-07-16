# web-auth Specification

## Purpose
TBD - created by archiving change unified-login-slice. Update Purpose after archive.
## Requirements
### Requirement: 统一 Web 登录入口

系统 SHALL 仅提供 **一个** 产品级登录页；管理员与普通员工使用同一登录接口与同一 JWT 会话。

#### Scenario: 登录成功后进入工作台

- **WHEN** 用户在 `/app/login` 提交有效凭据
- **THEN** 前端保存 access token
- **AND** 当仅解析出一个活跃租户时跳转至 `/app/messages`
- **AND** 当存在多个活跃租户且须选择时，在登录页或弹层中让用户选择企业后再进入工作台

#### Scenario: 未登录访问管理端

- **WHEN** 用户未持有 token 访问 `/admin` 下受保护路由
- **THEN** 重定向至 `/app/login` 并携带 `redirect` 查询参数

#### Scenario: 旧管理端登录 URL 兼容

- **WHEN** 用户访问 `/admin/login`
- **THEN** 重定向至 `/app/login`（保留 query）

#### Scenario: 禁止双登录入口

- **WHEN** 产品导航或首页展示登录入口
- **THEN** MUST NOT 同时提供独立的「管理员登录」与「员工登录」两个入口

### Requirement: 产品面未登录强制跳转登录

系统 SHALL 在用户未持有有效 token 时，禁止访问 `/app/**` 下除 `/app/login` 以外的路由；须重定向至 `/app/login` 并携带 `redirect` 查询参数。

#### Scenario: 未登录访问工作台

- **WHEN** 用户未持有 token 访问 `/app/messages`
- **THEN** 前端重定向至 `/app/login`
- **AND** `redirect` 查询参数包含原路径

#### Scenario: 未登录不渲染产品面壳层

- **WHEN** 用户未持有 token 访问任意 `/app/**` 受保护路由
- **THEN** 前端 MUST NOT 渲染 workspace 壳层后再提示未登录

### Requirement: 管理面未登录强制跳转登录

系统 SHALL 在用户未持有有效 token 时，禁止访问 `/admin/**`；须重定向至 `/app/login` 并携带 `redirect` 查询参数（**不**使用独立管理员登录页）。

#### Scenario: 未登录访问管理后台

- **WHEN** 用户未持有 token 访问 `/admin/system/user`
- **THEN** 前端重定向至 `/app/login`
- **AND** `redirect` 查询参数包含原路径

#### Scenario: 未登录不渲染管理端壳层

- **WHEN** 用户未持有 token 访问任意 `/admin/**` 路由
- **THEN** 前端 MUST NOT 渲染 admin 壳层后再提示未登录

### Requirement: 账号注册页

当多租户开放注册启用时，系统 SHALL 在工作台提供 `/app/register` 注册页，用于创建账号与企业。

#### Scenario: 注册表单字段

- **WHEN** 用户打开 `/app/register`
- **THEN** 页面展示手机号、密码、确认密码、昵称与企业名称（tenantName）字段
- **AND** 使用 workspace 认证布局

#### Scenario: 注册成功进入工作台

- **WHEN** 用户提交有效注册数据且 API 成功
- **THEN** 前端保存 access token 与活跃租户
- **AND** 跳转至 `/app/messages`

#### Scenario: 登录页注册入口

- **WHEN** 用户查看 `/app/login`
- **THEN** 提供指向 `/app/register` 的注册链接（如「没有账号？注册」）
- **AND** MUST NOT 以「收到邀请？设置密码加入」类文案作为唯一主入口

#### Scenario: 旧邀请接受页重定向

- **WHEN** 用户访问 `/app/invite/accept`
- **THEN** 前端重定向至 `/app/register`
- **AND** 可从 query 预填手机号

### Requirement: 工作台企业切换器

当用户拥有多个 ACTIVE 租户成员关系时，工作台 SHALL 支持企业切换；企业名称在资料卡片中展示，切换通过左下角多账号 Dock 完成，**不**在 workspace 头部单独展示 `WorkspaceTenantSwitcher`。

#### Scenario: 展示当前企业

- **WHEN** 已认证用户拥有一个或多个 ACTIVE 企业
- **THEN** 资料卡片展示当前企业名称
- **AND** Dock 高亮当前账号×企业 entry

#### Scenario: 切换企业

- **WHEN** 用户从 Dock 选择另一企业（同账号或已登录的另一账号 entry）
- **THEN** 前端调用企业切换 API 或恢复对应 token，并刷新租户范围 UI 状态（含 WebSocket 重连）

#### Scenario: 登录时企业选择

- **WHEN** 登录 API 返回 `TENANT_SELECTION_REQUIRED` 及企业列表
- **THEN** 登录页或弹层让用户选择企业后再进入工作台

### Requirement: Workspace profile card

The workspace shell SHALL expose a profile card opened from the bottom-left avatar, aligned with Feishu-style account UX.

#### Scenario: Open profile from avatar

- **WHEN** an authenticated user clicks the bottom-left avatar in the workspace rail
- **THEN** a popover shows large avatar, inline-editable nickname, current tenant name with an unverified badge (V1), logout, admin portal entry when `isAdmin`, and a link to add another account

#### Scenario: Inline nickname edit

- **WHEN** the user saves a new nickname in the profile card
- **THEN** the frontend calls `PUT /app-api/system/user/profile`
- **AND** updates local session and account-dock state on success

#### Scenario: Avatar upload from profile card

- **WHEN** the user uploads an image from the profile card
- **THEN** the frontend uses app-api infra upload session + confirm
- **AND** persists the returned `fileId` via `PUT /app-api/system/user/profile`
- **AND** displays the avatar via `/app-api/infra/file/public/{fileId}`

### Requirement: Workspace multi-account dock

The workspace shell SHALL persist multiple logged-in account×tenant sessions in browser localStorage and allow one-click switching.

#### Scenario: Dock entry shape

- **WHEN** the user logs in, switches tenant, or updates profile
- **THEN** the frontend upserts an entry keyed by `${userId}:${tenantId}` with token, tenant name, nickname, avatar, and `isAdmin`

#### Scenario: Switch dock entry same user

- **WHEN** the user selects another dock entry for the same `userId` but different `tenantId`
- **THEN** the frontend calls tenant switch API, updates token, refreshes permission info, and reconnects WebSocket

#### Scenario: Switch dock entry different user

- **WHEN** the user selects a dock entry for a different `userId`
- **THEN** the frontend restores that entry's token and tenant context without a new login round-trip

#### Scenario: Logout removes user entries

- **WHEN** the user logs out from the profile card
- **THEN** the frontend removes all dock entries for the current `userId`
- **AND** activates the next dock entry or redirects to `/app/login`

#### Scenario: Add another account

- **WHEN** the user chooses「登录更多账号」
- **THEN** the frontend navigates to `/app/login?addAccount=1`
- **AND** a successful login appends a new dock entry without discarding existing entries

### Requirement: Registration page pending invite banner

The registration page SHALL display pending enterprise invitations when a mobile number is entered, aligning with the Feishu-style "you have been invited" experience.

#### Scenario: Show invite banner on register

- **WHEN** a user enters a mobile on `/app/register` that has pending `NOT_JOINED` memberships
- **THEN** the page displays a banner listing the inviting enterprise names
- **AND** explains that registration will activate those memberships

#### Scenario: Hide banner without mobile

- **WHEN** the mobile field is empty
- **THEN** the invite banner is not shown

### Requirement: Workspace reaches users via IM bot conversations

The workspace product surface MUST present business reachability (invites, task due, approvals, etc.) as Bot conversations / messages under `/app/messages`, not as a separate notification bell inbox.

#### Scenario: No notification bell in shell

- **WHEN** an authenticated user opens the workspace shell
- **THEN** the rail MUST NOT render a notification-bell control backed by `infra_notify` or equivalent parallel inbox
- **AND** unread business reminders are visible via IM conversation list unread state for `bot_dm`

### Requirement: Registration SMS verification UI

When SMS verification is enabled, the registration page SHALL collect a verification code and support resend with cooldown.

> **实现状态**：`account-sms-verify` 已归档为规划规格，V1 前期暂缓实现。

#### Scenario: Show verification field when enabled

- **WHEN** the frontend detects SMS verification is required for registration
- **THEN** `/app/register` shows a verification code input and a "获取验证码" button

#### Scenario: Resend cooldown

- **WHEN** a user clicks send verification code
- **THEN** the button enters a countdown state (e.g. 60 seconds) before allowing resend

#### Scenario: Hide when SMS disabled

- **WHEN** SMS verification is not required
- **THEN** the registration form does not show the verification code fields

