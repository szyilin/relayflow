## ADDED Requirements

### Requirement: Infrastructure storage schema tables

The system MUST persist tenant storage configuration, file metadata, upload sessions, and file-to-business bindings in `infra_`-prefixed tables created by Flyway migration.

#### Scenario: Storage provider table exists

- **WHEN** Flyway migration `V0.1.0.5__init_infra_storage.sql` is applied
- **THEN** table `infra_storage_provider` exists with `tenant_id`, `provider`, `status`, `is_default`, and `config_json` columns
- **AND** standard audit columns (`creator`, `create_time`, `updater`, `update_time`, `deleted`) are present

#### Scenario: File metadata table exists

- **WHEN** the same migration is applied
- **THEN** table `infra_file` exists with `storage_uri`, `object_key`, `provider`, `access_level`, and size metadata columns

#### Scenario: Upload session table exists

- **WHEN** the same migration is applied
- **THEN** table `infra_file_upload_session` exists with `status` supporting `pending`, `confirmed`, and `expired`
- **AND** includes `expires_at` for session TTL

#### Scenario: File binding table exists

- **WHEN** the same migration is applied
- **THEN** table `infra_file_binding` exists linking `file_id` to `biz_type` and `biz_id` within a tenant

### Requirement: Infrastructure storage RBAC permission seeds

The system MUST seed RBAC permission codes for storage administration and file operations and bind them to the `super_admin` role for the default tenant.

#### Scenario: Storage permission codes seeded

- **WHEN** migration `V0.1.0.5__init_infra_storage.sql` is applied on a fresh database
- **THEN** permissions `infra:storage:query`, `infra:storage:update`, and `infra:storage:test` exist for tenant 1

#### Scenario: File permission codes seeded

- **WHEN** the same migration is applied
- **THEN** permissions `infra:file:list`, `infra:file:upload`, `infra:file:download`, and `infra:file:delete` exist for tenant 1

#### Scenario: Super admin receives infra permissions

- **WHEN** the same migration is applied
- **THEN** role `super_admin` (id 100) is granted all seeded `infra:storage:*` and `infra:file:*` permissions
