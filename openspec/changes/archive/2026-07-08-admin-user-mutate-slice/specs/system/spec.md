## ADDED Requirements

### Requirement: Admin user mutate API

The system SHALL provide admin endpoints to create users with optional dept and role assignment, query user detail, update profile fields, update tenant member status, update primary dept, and update role bindings; each endpoint SHALL require the corresponding `system:user:*` permission.

#### Scenario: Create user with dept and roles

- **WHEN** an admin with `system:user:create` POSTs valid user data with `deptId` and `roleIds`
- **THEN** the system creates the global user, tenant membership, dept relation, and role relations

#### Scenario: User page data scope filter

- **WHEN** an admin with limited data_scope requests `GET /admin-api/system/user/page`
- **THEN** the response list contains only users visible under the union of SELF and allowed deptIds (or all users when scope is ALL)
