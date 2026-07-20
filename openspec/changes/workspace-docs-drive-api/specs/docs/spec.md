## ADDED Requirements

### Requirement: Drive App API for personal folders and FILE registration

The system SHALL expose authenticated `/app-api/docs/drive` endpoints for the owner to manage personal Drive folders, list folder contents, register uploaded infra files as `FILE` objects, and update/delete Drive items, matching the workspace-docs-drive lane contract (excluding cross-container move).

#### Scenario: Create and list folder

- **WHEN** the owner creates a folder under a parent they own (or at root)
- **THEN** subsequent `GET /items` or `GET /folders` for that parent includes the folder

#### Scenario: Register FILE

- **WHEN** the owner posts `POST /drive/files` with a valid infra `fileId` they uploaded
- **THEN** the system creates a `FILE` `doc_object` with `storage_file_id` and a `doc_drive_item`
- **AND** binds the infra file to the document object

#### Scenario: Non-empty folder delete rejected

- **WHEN** the owner deletes a folder that still has child folders or items
- **THEN** the system rejects with `DOC_DRIVE_FOLDER_NOT_EMPTY`
