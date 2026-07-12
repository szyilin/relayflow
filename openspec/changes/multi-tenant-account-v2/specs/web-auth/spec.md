## ADDED Requirements

### Requirement: Account registration page

The system SHALL provide a workspace registration page at `/app/register` for open account and enterprise creation when multi-tenant registration is enabled.

#### Scenario: Registration form fields

- **WHEN** a user opens `/app/register`
- **THEN** the page shows fields for mobile, password, confirm password, nickname, and enterprise name (tenantName)
- **AND** uses the workspace auth layout

#### Scenario: Successful registration enters workspace

- **WHEN** the user submits valid registration data and the API succeeds
- **THEN** the frontend stores the access token and active tenant
- **AND** redirects to `/app/messages`

#### Scenario: Login page registration entry

- **WHEN** a user views `/app/login`
- **THEN** a link labeled for account registration (e.g. 「没有账号？注册」) points to `/app/register`
- **AND** MUST NOT use invite-only copy such as 「收到邀请？设置密码加入」 as the primary entry

#### Scenario: Legacy invite accept redirect

- **WHEN** a user navigates to `/app/invite/accept`
- **THEN** the frontend redirects to `/app/register`
- **AND** may pre-fill mobile from query parameters

### Requirement: Enterprise switcher in workspace shell

When the user belongs to more than one ACTIVE tenant, the workspace shell SHALL expose a tenant switcher.

#### Scenario: Display current enterprise

- **WHEN** an authenticated user has one or more ACTIVE tenants
- **THEN** the workspace header shows the current enterprise name

#### Scenario: Switch enterprise

- **WHEN** the user selects another enterprise from the switcher
- **THEN** the frontend calls tenant switch API, updates the token, and refreshes tenant-scoped UI state

#### Scenario: Login tenant selection

- **WHEN** login API returns `TENANT_SELECTION_REQUIRED` with a tenant list
- **THEN** the login page or a modal lets the user pick an enterprise before entering the workspace

## MODIFIED Requirements

### Requirement: Unified Web login entry

The system SHALL provide **one** product-level login page; administrators and employees use the same login API and JWT session.

#### Scenario: Login success enters workspace

- **WHEN** a user submits valid credentials at `/app/login`
- **THEN** the frontend stores the access token
- **AND** redirects to `/app/messages` when a single active tenant is resolved
- **AND** prompts for tenant selection when multiple active tenants require it

#### Scenario: Unauthenticated admin access

- **WHEN** a user without token visits a protected `/admin` route
- **THEN** redirect to `/app/login` with `redirect` query parameter

#### Scenario: Legacy admin login URL

- **WHEN** a user visits `/admin/login`
- **THEN** redirect to `/app/login` preserving query

#### Scenario: No dual login entry

- **WHEN** product navigation shows login entry points
- **THEN** MUST NOT provide separate 「admin login」 and 「employee login」 entries
