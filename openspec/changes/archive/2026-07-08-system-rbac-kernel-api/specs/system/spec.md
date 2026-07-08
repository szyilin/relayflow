## ADDED Requirements

### Requirement: Permission loading on authenticated requests

The system SHALL load the current user's effective permission codes from the database on each authenticated admin request and attach them to the security context as authorities equal to `sys_permission.code`.

#### Scenario: Super admin has seeded permissions

- **WHEN** user `admin` authenticates with a valid JWT
- **THEN** the security context authorities include `system:user:list`
- **AND** include other seeded system permission codes bound to `super_admin`

### Requirement: Permission info endpoint

The system SHALL expose `GET /admin-api/system/auth/get-permission-info` for any authenticated admin user to retrieve their roles and permission code list as defined in the slice contract.

#### Scenario: Successful permission info

- **WHEN** a client calls `get-permission-info` with a valid Bearer token
- **THEN** HTTP status is 200 and `code` is 0
- **AND** `data.permissions` is a non-empty string array for `super_admin`

#### Scenario: Unauthenticated permission info

- **WHEN** a client calls `get-permission-info` without a token
- **THEN** the system returns HTTP 401

## MODIFIED Requirements

### Requirement: RBAC runtime enforcement

The system SHALL enforce API access using `permission.code` loaded from `user → sys_user_role → sys_role → sys_role_permission → sys_permission`; JWT authentication alone MUST NOT grant access to permission-protected admin endpoints.

#### Scenario: User page requires list permission

- **WHEN** an authenticated user without `system:user:list` calls `GET /admin-api/system/user/page`
- **THEN** the system returns HTTP 403

#### Scenario: User page with list permission

- **WHEN** user `admin` with `system:user:list` calls `GET /admin-api/system/user/page`
- **THEN** the request succeeds with HTTP 200 and `code=0`
