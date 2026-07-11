## ADDED Requirements

### Requirement: Admin storage settings page

The system SHALL provide an admin page at `/admin/infra/storage` for managing tenant object storage provider configuration.

#### Scenario: View storage settings

- **WHEN** an admin with `infra:storage:query` opens `/admin/infra/storage`
- **THEN** the page displays current tenant storage provider settings returned by the storage config API
- **AND** secret keys MUST NOT be shown in plaintext

#### Scenario: Save storage settings

- **WHEN** an admin with `infra:storage:update` submits the storage configuration form
- **THEN** the page calls `PUT /admin-api/infra/storage/config` and shows success feedback

#### Scenario: Test storage connectivity from UI

- **WHEN** an admin with `infra:storage:test` clicks test connection
- **THEN** the page calls `POST /admin-api/infra/storage/test-connection` and displays the result

#### Scenario: Navigation entry

- **WHEN** an admin with `infra:storage:query` views the admin sidebar
- **THEN** a「存储设置」navigation item links to `/admin/infra/storage`
