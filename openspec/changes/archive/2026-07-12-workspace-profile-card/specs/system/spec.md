## ADDED Requirements

### Requirement: Workspace user profile API

The system SHALL expose `/app-api/system/user/profile` for authenticated workspace members to read and update their global profile fields without admin RBAC.

#### Scenario: Get current profile

- **WHEN** an authenticated workspace member calls `GET /app-api/system/user/profile`
- **THEN** the system returns `userId`, `username`, `nickname`, `avatar` (fileId string or empty), `tenantId`, `tenantName`, `tenantVerified` (V1 always `false`), and `isAdmin`
- **AND** the caller is not required to hold any `sys_permission` code

#### Scenario: Update nickname

- **WHEN** an authenticated member calls `PUT /app-api/system/user/profile` with a non-empty `nickname` (≤ 64 characters)
- **THEN** the system updates `sys_user.nickname`
- **AND** returns the same shape as GET profile

#### Scenario: Update avatar

- **WHEN** an authenticated member calls `PUT /app-api/system/user/profile` with `avatar` set to a valid tenant-scoped `fileId`
- **THEN** the system stores the fileId in `sys_user.avatar`
- **AND** returns the updated profile

#### Scenario: Reject empty nickname

- **WHEN** `PUT /app-api/system/user/profile` sets `nickname` to blank
- **THEN** the system rejects with business error `USER_NICKNAME_EMPTY`

### Requirement: Permission info includes avatar

The system SHALL include the current user's `avatar` fileId in `GET /admin-api/system/auth/get-permission-info` for session restore and workspace UI.

#### Scenario: Avatar in permission info

- **WHEN** an authenticated user calls `GET /admin-api/system/auth/get-permission-info`
- **THEN** the response includes `avatar` consistent with `sys_user.avatar`
