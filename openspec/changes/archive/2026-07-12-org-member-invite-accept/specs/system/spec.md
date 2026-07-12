## ADDED Requirements

### Requirement: Member invite acceptance

The system SHALL allow invited users to preview and accept pending tenant memberships via public app APIs, set their account password on first acceptance, transition `sys_tenant_user.status` from `NOT_JOINED` to `ACTIVE`, and issue a JWT equivalent to a successful login.

#### Scenario: Preview pending invite

- **WHEN** a client calls `GET /app-api/system/member-invite/preview` with a mobile that has `NOT_JOINED` membership in the default tenant
- **THEN** the response includes `tenantId`, `tenantName`, and `nickname`
- **AND** the endpoint does not require authentication

#### Scenario: Preview not found

- **WHEN** the mobile has no `NOT_JOINED` membership in the default tenant
- **THEN** the system returns a business error

#### Scenario: Accept invite and login

- **WHEN** a client calls `POST /app-api/system/member-invite/accept` with valid mobile and password for a `NOT_JOINED` member
- **THEN** the system sets the user's password hash
- **AND** updates `sys_tenant_user.status` to `ACTIVE`
- **AND** returns `accessToken` and `tenantId`
- **AND** subsequent login with the same credentials succeeds

#### Scenario: Accept weak password

- **WHEN** the submitted password is shorter than 6 characters
- **THEN** the system rejects the request with a business error
