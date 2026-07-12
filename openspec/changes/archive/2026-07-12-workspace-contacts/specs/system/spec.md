## ADDED Requirements

### Requirement: Workspace organization directory APIs

The system SHALL expose read-only `/app-api/system/` endpoints so authenticated workspace members can browse the tenant organization directory without admin RBAC checks.

#### Scenario: Department tree for workspace

- **WHEN** an authenticated workspace member requests `GET /app-api/system/dept/tree`
- **THEN** the system returns the current tenant's department hierarchy for navigation
- **AND** the caller is not required to hold any `sys_permission` code

#### Scenario: Members by department

- **WHEN** an authenticated workspace member requests `GET /app-api/system/user/list-by-dept` with a valid `deptId`
- **THEN** the system returns users whose primary department equals `deptId`
- **AND** each item includes fields sufficient for a contact card (e.g. id, nickname, department name, avatar text)

#### Scenario: Admin APIs remain separate

- **WHEN** a workspace member uses `/app-api/system/*` directory endpoints
- **THEN** no user mutation or admin-only fields are exposed
- **AND** write operations remain on `/admin-api/system/*` with RBAC
