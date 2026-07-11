## ADDED Requirements

### Requirement: File download routing

The system SHALL redirect clients to presigned object storage URLs for file downloads, with separate public and private entry points.

#### Scenario: Public file download without login

- **WHEN** a client requests `GET /app-api/infra/file/public/{fileId}` for a file with `access_level=public`
- **THEN** the system responds with HTTP 302 to a presigned GET URL
- **AND** includes `Cache-Control: public, max-age=31536000, immutable`

#### Scenario: Public endpoint rejects private file

- **WHEN** a client requests the public endpoint for a private file
- **THEN** the system rejects with a clear business error

#### Scenario: Admin private file download

- **WHEN** an admin with `infra:file:download` requests `GET /admin-api/infra/file/{fileId}/download`
- **THEN** the system responds with HTTP 302 to a presigned GET URL with 15-minute validity
