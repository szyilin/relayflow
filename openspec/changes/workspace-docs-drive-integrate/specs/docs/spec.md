## ADDED Requirements

### Requirement: Drive UI uses live App APIs

The workspace Drive panel SHALL load and mutate data via `/app-api/docs/drive` and infra upload/download. Temporary in-store mock Maps MUST be removed.

#### Scenario: Persist after refresh

- **WHEN** the owner creates a folder and uploads a file then reloads the Drive listing
- **THEN** the folder and file still appear from the server

---

### Requirement: Cross-container placement move

The system SHALL allow moving a document object between Library and Drive while preserving `doc_object.id`, with exactly one active placement afterward.

#### Scenario: Library RICH_DOC to Drive

- **WHEN** the owner moves a Library `RICH_DOC` into a Drive folder (or root)
- **THEN** the Library node is removed and a Drive item is created for the same `objectId`

#### Scenario: Drive RICH_DOC to Library

- **WHEN** the owner moves a Drive `RICH_DOC` back to the Library
- **THEN** the Drive item is removed and a Library node is created under the chosen parent

#### Scenario: FILE to Library rejected

- **WHEN** the owner attempts to move a `FILE` object into the Library
- **THEN** the system rejects the request
