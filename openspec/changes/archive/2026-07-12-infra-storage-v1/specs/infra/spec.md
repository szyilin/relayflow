## ADDED Requirements

### Requirement: Bootstrap default object storage validation

The system MUST validate the configured default object storage provider at application startup; if missing or incomplete, startup MUST fail.

#### Scenario: MinIO bootstrap config complete

- **WHEN** `relayflow.storage.default-provider=minio` and MinIO connection properties are complete
- **THEN** the application starts successfully
- **AND** tenants without custom storage use this bootstrap configuration

#### Scenario: Missing default provider

- **WHEN** `relayflow.storage.default-provider` is not set
- **THEN** application startup fails with a clear error message

#### Scenario: Local provider forbidden in production

- **WHEN** a non-dev profile sets `default-provider=local`
- **THEN** application startup fails

### Requirement: Tenant-configurable storage providers

The system SHALL allow tenants to configure one or more object storage providers in the admin console, designate one as default for new uploads, and resolve historical files by each file record's `storage_uri` / `provider` against the matching provider configuration.

#### Scenario: Save MinIO tenant config

- **WHEN** an admin submits valid MinIO connection parameters
- **THEN** the system persists the provider configuration with encrypted secrets
- **AND** the provider MAY be set as tenant default

#### Scenario: Switch default provider retains history

- **WHEN** a tenant switches default provider from MinIO to another configured provider
- **THEN** new uploads use the new default
- **AND** existing files with the old `provider` value remain readable via the legacy provider configuration

#### Scenario: Delete provider still referenced

- **WHEN** an admin attempts to delete a provider configuration still referenced by `infra_file` rows
- **THEN** the system rejects deletion with a clear error

#### Scenario: Test storage connectivity

- **WHEN** an admin calls the storage test-connection API
- **THEN** the system probes connectivity using submitted or saved parameters
- **AND** secrets MUST NOT be written to audit logs from test requests

### Requirement: Pluggable object storage strategy

The system MUST access object storage through a pluggable `ObjectStorageClient` strategy; business code MUST NOT depend directly on a vendor SDK.

#### Scenario: V1 MinIO implementation

- **WHEN** provider type is `minio`
- **THEN** the system uses the MinIO-compatible implementation for upload sessions and object I/O

#### Scenario: Unimplemented provider type

- **WHEN** provider type is an enum value without an implementation (e.g. `oss`)
- **THEN** the system returns an explicit error
- **AND** MUST NOT silently fall back to local disk

### Requirement: Direct upload session

The system SHALL support upload sessions so clients write file bytes directly to object storage without the application server proxying file content by default.

#### Scenario: Create upload session

- **WHEN** an authenticated user with upload permission requests a session with filename, size, and MIME type
- **THEN** the system returns `uploadId`, a presigned upload URL, object key, and expiry
- **AND** the object key MUST include the `tenant/{tenantId}/` prefix

#### Scenario: Confirm upload

- **WHEN** the client submits `uploadId` and object metadata (e.g. ETag, size) after a successful direct upload
- **THEN** the system verifies the object exists and size matches
- **AND** creates an `infra_file` metadata row and returns `fileId`

#### Scenario: Business APIs reference fileId only

- **WHEN** another module associates an attachment with business data
- **THEN** business API request bodies contain only `fileId` (or lists thereof)
- **AND** multipart file streams MUST NOT be the default upload path for business APIs

### Requirement: File business binding

The system SHALL support binding `infra_file` records to business entities for authorization and lifecycle management.

#### Scenario: Create binding

- **WHEN** a business service binds a file with `fileId`, `bizType`, and `bizId`
- **THEN** the system persists an `infra_file_binding` row

### Requirement: File download access levels

The system MUST distinguish public and private download paths by each file's `access_level`.

#### Scenario: Public file download

- **WHEN** a public file with `access_level=public` is requested
- **THEN** the system returns a short-lived object storage read URL or equivalent redirect
- **AND** the response SHOULD allow long-lived browser caching (e.g. avatars)

#### Scenario: Private file download

- **WHEN** a user requests a file with `access_level=private`
- **THEN** the system MUST validate JWT and permission (or business binding authorization) first
- **AND** then return a short-lived presigned read URL or HTTP 302 redirect

## MODIFIED Requirements

### Requirement: Object storage file upload

The system SHALL store uploaded files in MinIO-compatible object storage and return an accessible URL or file ID; large file bytes MUST NOT be proxied through the application server by default.

#### Scenario: Direct upload attachment

- **WHEN** an authenticated user completes a direct upload via upload session and confirm
- **THEN** the file is written to object storage
- **AND** metadata is recorded in an `infra_`-prefixed table
- **AND** a `fileId` is returned for business reference

#### Scenario: Upload permission denied

- **WHEN** a user without `infra:file:upload` attempts to create an upload session
- **THEN** the system returns HTTP 403
