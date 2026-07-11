## ADDED Requirements

### Requirement: Admin file list and delete UI

The system SHALL provide an admin file management page at `/admin/infra/file` with paginated list, direct upload, and delete actions wired to infra file APIs.

#### Scenario: List files with permission

- **WHEN** an admin with `infra:file:list` opens `/admin/infra/file`
- **THEN** the page displays a paginated table of `infra_file` metadata for the current tenant

#### Scenario: Direct upload from admin page

- **WHEN** an admin with `infra:file:upload` selects a file and uploads
- **THEN** the client uses presigned PUT via `useDirectUpload` and refreshes the list after confirm

#### Scenario: Delete file metadata

- **WHEN** an admin with `infra:file:delete` confirms delete on a file row
- **THEN** the system logically deletes the `infra_file` row and removes it from the list
