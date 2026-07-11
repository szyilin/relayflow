## ADDED Requirements

### Requirement: Direct upload session and confirm

The system SHALL support a three-phase direct upload flow for admin file uploads: create upload session, client PUT to presigned URL, then confirm to persist `infra_file` metadata.

#### Scenario: Create upload session

- **WHEN** an admin with `infra:file:upload` calls `POST /admin-api/infra/file/upload-session` with valid `filename`, `size`, and `mimeType`
- **THEN** the system persists an `infra_file_upload_session` row with `status=pending`
- **AND** returns `uploadId`, `mode=presigned_put`, `objectKey`, `uploadUrl`, `headers`, and `expiresAt`
- **AND** the presigned PUT MUST be generated via `ObjectStorageClient` using the tenant-effective storage configuration

#### Scenario: Confirm uploaded file

- **WHEN** an admin with `infra:file:upload` calls `POST /admin-api/infra/file/confirm` with a valid pending `uploadId` and matching `size`
- **AND** the object exists in object storage with matching size per `headObject`
- **THEN** the system inserts an `infra_file` row and marks the session `confirmed`
- **AND** returns `fileId` and `storageUri`

#### Scenario: Confirm rejects expired session

- **WHEN** confirm is called after `expires_at`
- **THEN** the system rejects with a clear business error
- **AND** the session status MAY be updated to `expired`

#### Scenario: Cross-module file access

- **WHEN** another module calls `FileApi.getFile(fileId)` for a file in the current tenant
- **THEN** the system returns file metadata without exposing storage secrets

#### Scenario: Bind file to business entity

- **WHEN** a module calls `FileApi.bindFile` with `fileId`, `bizType`, and `bizId`
- **THEN** the system persists an `infra_file_binding` row for the current tenant

#### Scenario: Permission denied

- **WHEN** a user without `infra:file:upload` calls upload-session or confirm
- **THEN** the system returns HTTP 403
