## MODIFIED Requirements

### Requirement: 用户写操作管理端 API

The system SHALL provide admin APIs to create users (with department and role assignment), query detail, update profile, update member status, update primary department, and update role bindings; user paging SHALL be filtered by the caller's effective data scope.

#### Scenario: 创建用户并分配部门与角色

- **WHEN** an administrator with `system:user:create` submits a valid create request with department and role assignments
- **THEN** the system creates `sys_user`, `sys_tenant_user`, and related association rows

#### Scenario: 用户分页按 data_scope 过滤

- **WHEN** a user with `system:user:list` and non-ALL data scope requests the user page API
- **THEN** returned users are limited to the caller's effective data scope (union of SELF and allowed deptIds, or full set when ALL)

#### Scenario: 用户分页按部门过滤

- **WHEN** an administrator requests `GET /admin-api/system/user/page` with query `deptId`
- **THEN** the response list contains only users whose **primary** department (`sys_user_dept.primary_flag = 1`) equals `deptId`
- **AND** results remain subject to the caller's data scope rules
