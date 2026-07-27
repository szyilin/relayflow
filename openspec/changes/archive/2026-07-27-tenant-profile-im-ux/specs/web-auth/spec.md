## MODIFIED Requirements

### Requirement: Workspace multi-account dock

The workspace SHALL persist a multi-account / multi-tenant dock keyed by `${userId}:${tenantId}` with tenant name, **tenant-scoped** nickname, avatar, and `isAdmin`.

#### Scenario: Dock entry per enterprise

- **WHEN** the user logs in, switches tenant, or updates profile
- **THEN** the frontend upserts only the current `${userId}:${tenantId}` entry with that tenant's nickname and avatar
- **AND** MUST NOT copy the current avatar/nickname onto other tenant entries of the same userId

#### Scenario: Avatar fallback without broken image

- **WHEN** a dock or「登录更多账号」row has empty or unloadable avatar
- **THEN** the UI shows a text/color tile fallback
- **AND** MUST NOT show a browser broken-image icon

### Requirement: Workspace profile card

The workspace shell SHALL expose a profile card opened from the bottom-left avatar, aligned with Feishu-style account UX. Profile fields shown and edited are **current-tenant member** fields.

#### Scenario: Open profile from avatar

- **WHEN** an authenticated user clicks the bottom-left avatar in the workspace rail
- **THEN** a popover shows large avatar, inline-editable nickname, current tenant name with an unverified badge (V1), logout, admin portal entry when `isAdmin`, and a link to add another account

#### Scenario: Inline nickname edit

- **WHEN** the user saves a new nickname in the profile card
- **THEN** the frontend calls `PUT /app-api/system/user/profile`
- **AND** only the current dock entry nickname updates

#### Scenario: Avatar upload from profile card

- **WHEN** the user uploads an image from the profile card
- **THEN** the frontend uploads via public file API
- **AND** persists the returned `fileId` via `PUT /app-api/system/user/profile`
- **AND** displays the avatar via `/app-api/infra/file/public/{fileId}`
- **AND** other enterprises under the same account keep their own avatars
