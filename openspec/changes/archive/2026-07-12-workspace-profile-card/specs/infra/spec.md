## ADDED Requirements

### Requirement: Workspace member file upload

The system SHALL allow any authenticated workspace member to create upload sessions and confirm public files via `/app-api/infra/file/` without `infra:file:upload` admin permission, for use cases such as profile avatars.

#### Scenario: App upload session without admin permission

- **WHEN** an authenticated workspace member calls `POST /app-api/infra/file/upload-session` with filename, size, mimeType, and `accessLevel=public`
- **THEN** the system returns `uploadId`, presigned upload URL, objectKey, and expiry
- **AND** the caller is not required to hold `infra:file:upload`

#### Scenario: App upload confirm

- **WHEN** the member completes direct upload and calls `POST /app-api/infra/file/upload-confirm`
- **THEN** the system creates `infra_file` metadata and returns `fileId`
- **AND** the file is scoped to the JWT tenant
