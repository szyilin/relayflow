## ADDED Requirements

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

## MODIFIED Requirements

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
