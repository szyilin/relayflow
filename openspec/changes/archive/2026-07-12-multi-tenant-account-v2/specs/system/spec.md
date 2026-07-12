## ADDED Requirements

### Requirement: Open account registration

When `relayflow.tenant.enabled=true` and `relayflow.tenant.allow-open-register=true`, the system SHALL expose a public app API for account registration that creates a global user, a new tenant owned by that user, and an ACTIVE tenant membership.

#### Scenario: Register with new mobile creates enterprise

- **WHEN** a client calls `POST /app-api/system/auth/register` with a unique mobile, valid password (≥6 characters), nickname, and tenantName
- **THEN** the system creates `sys_user` with `username` equal to mobile and a BCrypt password hash
- **AND** creates a new `sys_tenant` with `name=tenantName` and `owner_user_id` set to the new user
- **AND** creates `sys_tenant_user` with status `ACTIVE` for the new tenant
- **AND** bootstraps minimum tenant structure (root department, owner primary dept, super_admin role binding)
- **AND** returns `accessToken` and `tenantId` for the newly created tenant
- **AND** the endpoint does not require authentication

#### Scenario: Register rejects duplicate mobile

- **WHEN** a client calls `POST /app-api/system/auth/register` with a mobile already bound to a user who has set a login password
- **THEN** the system rejects with business error `USER_MOBILE_EXISTS`

#### Scenario: Register rejects weak password

- **WHEN** password is missing or shorter than 6 characters
- **THEN** the system rejects with business error `AUTH_REGISTER_PASSWORD_WEAK`

#### Scenario: Register activates pending invites on other tenants

- **WHEN** the mobile has `NOT_JOINED` memberships on one or more tenants (created by admin invite) at registration time
- **THEN** the system sets the user password
- **AND** transitions all such `sys_tenant_user` rows to `ACTIVE`
- **AND** still creates the user's own new tenant when `tenantName` is provided
- **AND** the issued JWT uses the newly created tenant as the active tenant

#### Scenario: Open register disabled

- **WHEN** `relayflow.tenant.allow-open-register=false`
- **THEN** `POST /app-api/system/auth/register` is rejected or not registered

### Requirement: Multi-tenant login and tenant selection

When `relayflow.tenant.enabled=true`, the system SHALL authenticate global credentials and bind the session to one ACTIVE tenant membership.

#### Scenario: Login with single ACTIVE tenant

- **WHEN** a user submits valid credentials to `POST /admin-api/system/auth/login` and has exactly one ACTIVE `sys_tenant_user`
- **THEN** the system returns JWT with `tenant_id` of that membership

#### Scenario: Login with multiple tenants requires selection

- **WHEN** a user submits valid credentials and has more than one ACTIVE tenant membership without `tenantId` in the request
- **THEN** the system returns business error `TENANT_SELECTION_REQUIRED`
- **AND** includes a list of selectable tenants (`tenantId`, `tenantName`)

#### Scenario: Login with explicit tenant

- **WHEN** a user submits valid credentials and a valid `tenantId` for an ACTIVE membership
- **THEN** the system returns JWT with that `tenant_id`

#### Scenario: Login with mobile identifier

- **WHEN** the login identifier matches `sys_user.mobile` instead of `username`
- **THEN** authentication succeeds equivalently to username login

#### Scenario: Login without ACTIVE tenant

- **WHEN** credentials are valid but the user has no ACTIVE tenant membership
- **THEN** the system rejects with business error `AUTH_NO_TENANT`

### Requirement: Tenant switch for authenticated users

The system SHALL allow an authenticated user to switch the active tenant and receive a new JWT.

#### Scenario: List my tenants

- **WHEN** an authenticated user calls `GET /app-api/system/tenant/my-list`
- **THEN** the response lists all tenants where the user has `ACTIVE` membership
- **AND** each item includes `tenantId`, `tenantName`, and whether the user is `owner`

#### Scenario: Switch tenant

- **WHEN** an authenticated user calls `POST /app-api/system/tenant/switch` with a `tenantId` of an ACTIVE membership
- **THEN** the system returns a new `accessToken` with updated `tenant_id`
- **AND** subsequent API calls use the new tenant context

#### Scenario: Switch to forbidden tenant

- **WHEN** the user is not an ACTIVE member of the requested tenant
- **THEN** the system rejects with business error `TENANT_SWITCH_FORBIDDEN`

### Requirement: Tenant-enabled request context

When `relayflow.tenant.enabled=true`, the system SHALL derive tenant context from JWT for authenticated business APIs.

#### Scenario: JWT tenant drives MyBatis filter

- **WHEN** `enabled=true` and a request carries a valid JWT
- **THEN** `TenantContextHolder` is set from JWT `tenant_id`
- **AND** tenant-scoped SQL uses that tenant id

#### Scenario: Single-tenant mode ignores JWT tenant

- **WHEN** `enabled=false`
- **THEN** tenant context remains the configured `default-id` regardless of JWT claim

## MODIFIED Requirements

### Requirement: JWT user authentication

The system SHALL authenticate users by username or password and issue JWTs for subsequent API requests; JWT MUST include the active tenant identifier `tenant_id`.

#### Scenario: Login success

- **WHEN** a registered user with ACTIVE tenant membership submits valid credentials to `/admin-api/system/auth/login`
- **THEN** the system returns a JWT access token
- **AND** the token payload contains `tenant_id` of the active tenant (in multi-tenant mode) or default tenant (in single-tenant mode)
- **AND** subsequent requests with `Authorization: Bearer <token>` are accepted

#### Scenario: Invalid credentials

- **WHEN** a login attempt uses wrong password
- **THEN** the system rejects with unauthorized error
- **AND** no token is issued

### Requirement: Admin invite scoped to current tenant

The system SHALL provide admin API to invite members by mobile; admins MUST NOT set account passwords; invited members start as `NOT_JOINED` in the **current admin tenant context**.

#### Scenario: Invite by mobile

- **WHEN** an admin calls `POST /admin-api/system/user/invite` with a valid mobile and optional org fields
- **THEN** the system creates or reuses global `sys_user` for that mobile without accepting an admin-provided password
- **AND** creates `sys_tenant_user` with status `NOT_JOINED` for the **current tenant from admin JWT** when multi-tenant mode is enabled
- **AND** assigns primary department and roles within that tenant
- **AND** the member MUST NOT log in to that tenant until status becomes `ACTIVE`

#### Scenario: Duplicate invite for existing member

- **WHEN** the mobile already belongs to the current tenant
- **THEN** the system rejects the invite

## REMOVED Requirements

### Requirement: Member invite acceptance

**Reason**: Invite acceptance is merged into open registration and login activation; standalone `/app/invite/accept` and `member-invite/accept` are deprecated in V2 multi-tenant mode.

**Migration**: Use `POST /app-api/system/auth/register` (new users) or login after admin invite; `/app/invite/accept` redirects to `/app/register`.

#### Scenario: Preview pending invite

- **WHEN** a client calls `GET /app-api/system/member-invite/preview` with a mobile that has `NOT_JOINED` membership in the default tenant
- **THEN** *(removed in V2 — use register flow)*

#### Scenario: Preview not found

- **WHEN** mobile has no `NOT_JOINED` membership in default tenant
- **THEN** *(removed in V2)*

#### Scenario: Accept invite and login

- **WHEN** a client calls `POST /app-api/system/member-invite/accept` with valid mobile and password for a `NOT_JOINED` member
- **THEN** *(removed in V2 — use register)*

#### Scenario: Accept with weak password

- **WHEN** password is too short on accept
- **THEN** *(removed in V2)*
