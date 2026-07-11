## ADDED Requirements

### Requirement: Tenant storage configuration admin API

The system SHALL expose admin APIs for tenants to manage object storage provider configurations stored in `infra_storage_provider`.

#### Scenario: List tenant storage providers

- **WHEN** an admin with `infra:storage:query` calls `GET /admin-api/infra/storage/config`
- **THEN** the system returns all provider configurations for the current tenant
- **AND** secret keys MUST NOT be returned in plaintext

#### Scenario: Save MinIO provider configuration

- **WHEN** an admin with `infra:storage:update` submits valid MinIO parameters via `PUT /admin-api/infra/storage/config`
- **THEN** the system persists or updates the provider row with encrypted `secret_key_enc` inside `config_json`
- **AND** if `isDefault` is true, other tenant providers are cleared as default and the previous default MAY be marked `legacy`

#### Scenario: Delete provider still referenced by files

- **WHEN** an admin attempts to delete a provider configuration still referenced by `infra_file` rows
- **THEN** the system rejects deletion with a clear business error

#### Scenario: Test storage connectivity

- **WHEN** an admin with `infra:storage:test` calls `POST /admin-api/infra/storage/test-connection` with valid parameters or an existing saved provider
- **THEN** the system probes object storage connectivity using `ObjectStorageClient.checkConnectivity`
- **AND** secrets MUST NOT be written to application logs from the test request

#### Scenario: Permission denied

- **WHEN** a user without the required `infra:storage:*` permission calls any storage config API
- **THEN** the system returns HTTP 403
