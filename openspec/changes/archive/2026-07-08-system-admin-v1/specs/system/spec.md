## ADDED Requirements

### Requirement: RBAC runtime enforcement

The system SHALL enforce API access using `permission.code` loaded from `user → sys_user_role → sys_role → sys_role_permission → sys_permission`; JWT authentication alone MUST NOT grant access to permission-protected admin endpoints.

#### Scenario: Authorized API call

- **WHEN** an authenticated user calls an admin endpoint annotated with required permission `system:user:list`
- **AND** the user's effective permission set includes `system:user:list`
- **THEN** the request succeeds with HTTP 200 and `code=0`

#### Scenario: Missing permission

- **WHEN** an authenticated user calls an admin endpoint requiring `system:dept:create`
- **AND** the user's effective permission set does not include that code
- **THEN** the system returns HTTP 403

### Requirement: Permission info API

The system SHALL expose an authenticated endpoint for the current user to retrieve roles and permission codes for frontend menu and button gating.

#### Scenario: Fetch permission info

- **WHEN** a client requests `GET /admin-api/system/auth/get-permission-info` with a valid Bearer JWT
- **THEN** the response `code` is 0
- **AND** `data` includes `permissions` as a string array of `permission.code` values
- **AND** `data` includes basic user identity fields

### Requirement: Department management API

The system SHALL provide authenticated admin APIs to list, create, update, and delete departments within the current tenant, protected by `system:dept:*` permission codes.

#### Scenario: List department tree

- **WHEN** a user with `system:dept:list` requests the department list API
- **THEN** the system returns a hierarchical department tree for the current tenant

#### Scenario: Delete department with children

- **WHEN** a user attempts to delete a department that has child departments or assigned users
- **THEN** the system rejects the operation with a business error

### Requirement: Role management API

The system SHALL provide authenticated admin APIs to manage roles, bind permission points, and configure `data_scope` (and `sys_role_dept` when CUSTOM), protected by `system:role:*` permission codes.

#### Scenario: Create custom role with permissions

- **WHEN** a user with `system:role:create` submits a new role with selected permission IDs
- **THEN** the role is persisted with `role_type=CUSTOM`
- **AND** `sys_role_permission` rows are created

#### Scenario: System role cannot be deleted

- **WHEN** a user attempts to delete a role with `role_type=SYSTEM`
- **THEN** the system rejects the operation

#### Scenario: Child role permission subset

- **WHEN** role B's `parent_id` points to role A
- **THEN** role B's bound permissions MUST be a subset of role A's permissions

### Requirement: User mutation API

The system SHALL provide admin APIs to create and update users, assign departments and roles, and filter user list queries by the caller's effective data scope.

#### Scenario: Create user with dept and roles

- **WHEN** a user with `system:user:create` submits a valid create request with department and role assignments
- **THEN** the system creates `sys_user`, `sys_tenant_user`, and related join rows

#### Scenario: User list filtered by data scope

- **WHEN** a user with `system:user:list` and non-ALL data scope requests the user page API
- **THEN** returned users MUST be limited to records within the caller's effective data scope

### Requirement: Frontend permission gating

The admin frontend SHALL load permission codes after login and hide navigation entries (and optionally block routes) when the user lacks the associated permission code.

#### Scenario: Sidebar hides unauthorized menu

- **WHEN** the logged-in user's permission set does not include `system:role:list`
- **THEN** the admin sidebar MUST NOT show the role management entry

#### Scenario: Super admin sees all seeded entries

- **WHEN** the logged-in user has all system permissions (e.g. `super_admin`)
- **THEN** all V1 static admin nav entries gated by permission are visible
