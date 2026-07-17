## MODIFIED Requirements

### Requirement: Workspace multi-account dock

The workspace shell SHALL persist multiple logged-in account×tenant sessions in browser localStorage and allow one-click switching. V1 MAY store bearer tokens in dock entries to enable switch-without-relogin for different users; the project docs MUST record that XSS can exfiltrate all docked JWTs. Same-user cross-tenant switches MUST call the tenant switch API and refresh the active token rather than treating a stale copied token as sufficient. Logout MUST remove all dock entries for the current `userId`. Migrating dock credentials to httpOnly cookies or server-side opaque session ids is out of band for this requirement's V1 behavior but MUST be documented as the target hardening path.

#### Scenario: Dock entry shape

- **WHEN** the user logs in, switches tenant, or updates profile
- **THEN** the frontend upserts an entry keyed by `${userId}:${tenantId}` with tenant name, nickname, avatar, and `isAdmin`
- **AND** V1 entries MAY include a `token` field for cross-user one-click restore
- **AND** documentation notes the XSS multi-token risk

#### Scenario: Switch dock entry same user

- **WHEN** the user selects another dock entry for the same `userId` but different `tenantId`
- **THEN** the frontend calls tenant switch API, updates token, refreshes permission info, and reconnects WebSocket
- **AND** MUST NOT complete the switch solely by restoring a previously copied JWT without calling switch when a switch API is required

#### Scenario: Switch dock entry different user

- **WHEN** the user selects a dock entry for a different `userId`
- **THEN** the frontend restores that entry's token and tenant context without a new login round-trip

#### Scenario: Logout removes user entries

- **WHEN** the user logs out from the profile card
- **THEN** the frontend removes all dock entries for the current `userId`
- **AND** activates the next dock entry or redirects to `/app/login`

#### Scenario: Add another account

- **WHEN** the user chooses「登录更多账号」
- **THEN** the frontend navigates to `/app/login?addAccount=1`
- **AND** a successful login appends a new dock entry without discarding existing entries

#### Scenario: Threat model is discoverable

- **WHEN** a developer reads workspace frontend session documentation
- **THEN** the multi-account dock JWT-in-localStorage threat model and target hardening path are described
