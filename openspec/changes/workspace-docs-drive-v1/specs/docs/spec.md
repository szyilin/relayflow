## ADDED Requirements

### Requirement: Personal Drive folder tree

The system SHALL provide each active tenant member an implicit personal Drive («我的文件夹») organized as a folder tree. V1 MUST allow the owner to create, rename, move within their Drive, and soft-delete folders. Shared Drive folders are out of scope for V1.

#### Scenario: Empty Drive

- **WHEN** a member opens Drive and has never created a folder or uploaded a file
- **THEN** the Drive root listing is empty
- **AND** the system MUST NOT require a pre-inserted Drive header row

#### Scenario: Create folder

- **WHEN** the owner creates a folder under a parent folder they own (or at root)
- **THEN** the folder appears in subsequent listings under that parent

#### Scenario: Non-empty delete rejected by default

- **WHEN** the owner deletes a folder that still contains child folders or Drive items
- **THEN** the system rejects the delete with a documented business error
- **AND** MUST NOT silently cascade-delete contents in V1 (unless a later explicit decision changes this)

#### Scenario: Non-owner forbidden

- **WHEN** a member attempts to mutate another user’s Drive folder
- **THEN** the system rejects with forbidden or not-found

---

### Requirement: FILE object type in Drive

The system SHALL support `doc_object.type = FILE` for binary files placed in Drive. File bytes MUST be stored via the existing infra object storage (`infra_file` + MinIO). The docs module MUST reference the infra file id and MUST NOT store file bytes in `doc_object.body`.

#### Scenario: Register uploaded file into Drive

- **WHEN** the owner completes an app-api infra upload and registers the resulting `fileId` into a Drive folder they own
- **THEN** the system creates a `FILE` document object bound to that infra file
- **AND** creates a Drive placement under the target folder (or root)
- **AND** the file appears in that folder’s listing

#### Scenario: Download FILE

- **WHEN** the owner downloads a Drive FILE they own
- **THEN** the client obtains the file content via the existing workspace infra download mechanism (or an equivalent authorized redirect)

#### Scenario: Unsupported object types for upload registration

- **WHEN** a client attempts to register a Drive file with a non-`FILE` object type in this flow
- **THEN** the system rejects the request

---

### Requirement: Drive listing mixes folders and placed objects

Drive listings SHALL return child folders and placed document objects (at least `FILE`; `RICH_DOC` when moved into Drive) with enough metadata for the UI to branch (open vs download).

#### Scenario: List folder contents

- **WHEN** the owner lists a folder
- **THEN** the response includes child folders and Drive items for objects placed in that folder
- **AND** each object item includes `objectId`, `type`, and `title`

---

### Requirement: Cross-container move between Library and Drive

The system SHALL allow the owner to move a document object between Library and Drive placements while preserving the same `doc_object.id`. After a move, the object MUST appear in exactly one container listing.

#### Scenario: Move RICH_DOC from Library to Drive

- **WHEN** the owner moves a Library `RICH_DOC` into a Drive folder they own
- **THEN** the Library node for that object is removed (soft-deleted or equivalent)
- **AND** a Drive placement is created under the target folder
- **AND** the object id is unchanged
- **AND** the document no longer appears in the Library tree

#### Scenario: Move FILE from Drive to Library rejected or supported explicitly

- **WHEN** the owner attempts to move a `FILE` object into the Library
- **THEN** either the system rejects with a documented error (Library V1 pages are document-oriented)
- **OR** a later approved design allows it — V1 default MUST be **reject** `FILE` → Library

#### Scenario: No dual placement

- **WHEN** an object is placed in Drive
- **THEN** it MUST NOT simultaneously retain an active Library placement

---

### Requirement: Workspace Drive UI entry

The workspace `/app/docs` navigation SHALL enable the Drive entry for「我的文件夹」browse/upload flows. Shared folders and Wiki MAY remain placeholders.

#### Scenario: Drive panel usable

- **WHEN** a logged-in member opens `/app/docs` and selects Drive
- **THEN** they can browse their folder tree, create folders, and upload files once APIs are integrated
- **AND** Shared / Wiki entries MUST NOT imply full availability if still placeholders
