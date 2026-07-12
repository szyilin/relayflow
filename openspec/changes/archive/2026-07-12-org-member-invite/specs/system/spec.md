## MODIFIED Requirements

### Requirement: Admin user management

The system SHALL provide admin APIs to invite members by mobile (without setting account password), query detail, update org-scoped profile fields, update member status, update primary department, and update role bindings; user paging SHALL be filtered by the caller's effective data scope.

#### Scenario: Invite member by mobile

- **WHEN** an admin calls `POST /admin-api/system/user/invite` with a valid mobile and optional org fields
- **THEN** the system creates or reuses a global `sys_user` for that mobile without accepting a password from the admin
- **AND** creates `sys_tenant_user` with status `NOT_JOINED`
- **AND** assigns primary department and roles within the current tenant
- **AND** the invited member SHALL NOT be able to log in to this tenant until status becomes `ACTIVE`

#### Scenario: Invite duplicate member

- **WHEN** the mobile already belongs to an existing member of the current tenant
- **THEN** the system rejects the invite with a business error

#### Scenario: Member status in page response

- **WHEN** an admin queries `GET /admin-api/system/user/page`
- **THEN** each row includes `memberStatus` (enum name) and `mobile`
- **AND** `memberStatus` reflects `sys_tenant_user.status` including `NOT_JOINED` and `ACTIVE`
