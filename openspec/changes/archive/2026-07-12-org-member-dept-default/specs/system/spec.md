## MODIFIED Requirements

### Requirement: 组织架构

The system SHALL support an organization department tree and user-department membership; departments are used for org affiliation and data-scope calculation and SHALL NOT replace RBAC functional permissions.

#### Scenario: 部门树

- **WHEN** an authenticated administrator requests the department tree
- **THEN** the system returns hierarchical `sys_dept` rows for the current tenant
- **AND** each row includes `parent_id` to express hierarchy

#### Scenario: 用户多部门

- **WHEN** a user belongs to multiple departments
- **THEN** associations are stored in `sys_user_dept`
- **AND** at least one department MAY be marked as primary via `primary_flag`

#### Scenario: 列出部门

- **GIVEN** an authenticated administrator
- **WHEN** the department tree is requested
- **THEN** the system returns the hierarchical department list for the current tenant

#### Scenario: 租户根部门

- **WHEN** a tenant is initialized for organization features
- **THEN** the tenant SHALL have exactly one root department with `parent_id = 0`
- **AND** the root department display name SHOULD align with `sys_tenant.name` as the default organization node (Feishu-style company root)

#### Scenario: 根部门不可删除

- **WHEN** an administrator attempts to delete a root department (`parent_id = 0`)
- **THEN** the system rejects the operation

### Requirement: 用户写操作管理端 API

The system SHALL provide admin APIs to create users (with department and role assignment), query detail, update profile, update member status, update primary department, and update role bindings; user paging SHALL be filtered by the caller's effective data scope.

#### Scenario: 创建用户并分配部门与角色

- **WHEN** an administrator with `system:user:create` submits a valid create request with department and role assignments
- **THEN** the system creates `sys_user`, `sys_tenant_user`, and related association rows

#### Scenario: 创建用户未指定部门

- **WHEN** an administrator creates a user without `deptId`
- **THEN** the system assigns the tenant root department as the user's primary department (`sys_user_dept.primary_flag = 1`)

#### Scenario: 有效成员必有主部门

- **WHEN** a user is an active member of the current tenant (`sys_tenant_user` with login-allowed status)
- **THEN** the user SHALL have at least one `sys_user_dept` row with `primary_flag = 1`

#### Scenario: 禁止清空主部门

- **WHEN** an administrator updates a user's primary department with a null or missing `deptId`
- **THEN** the system rejects the request

#### Scenario: 用户分页按 data_scope 过滤

- **WHEN** a user with `system:user:list` and non-ALL data scope requests the user page API
- **THEN** returned users are limited to the caller's effective data scope (union of SELF and allowed deptIds, or full set when ALL)
