## ADDED Requirements

### Requirement: Multi-tenant product mode configuration

Deployment configuration SHALL support enabling multi-tenant product behavior without code changes.

#### Scenario: Enable multi-tenant mode

- **WHEN** `relayflow.tenant.enabled=true` is set in application configuration
- **THEN** the runtime derives tenant context from JWT for authenticated requests
- **AND** open registration is available when `relayflow.tenant.allow-open-register=true`

#### Scenario: Preserve single-tenant self-hosted mode

- **WHEN** `relayflow.tenant.enabled=false` (default for existing self-hosted installs)
- **THEN** behavior remains single default tenant (`default-id`, typically `1`)
- **AND** no tenant switcher or open registration is required for existing deployments

#### Scenario: Document environment variables

- **WHEN** operators configure Docker Compose or environment-based deployment
- **THEN** documentation lists `RELAYFLOW_TENANT_ENABLED` and `RELAYFLOW_TENANT_ALLOW_OPEN_REGISTER` (or equivalent property names) for V2 mode

## MODIFIED Requirements

### Requirement: Default tenant seed

The system SHALL seed a protected default tenant on first migration for bootstrap and single-tenant compatibility.

#### Scenario: Default tenant created

- **WHEN** Flyway runs initial tenant migration
- **THEN** a default tenant record exists (e.g. `id=1`, `code=default`)
- **AND** it cannot be deleted by application logic
- **AND** new user registrations in multi-tenant mode create **additional** tenants without replacing the seed
