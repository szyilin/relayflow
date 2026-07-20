# docs Specification

## Purpose

云文档域：三大存放容器（我的文档库 / 云盘 / 知识库）边界、文档本体与块模型，以及工作台 `/app/docs` 我的文档库（Library）行为。云盘与知识库能力由后续 change 增量扩展。

## Requirements

### Requirement: Docs domain module

The system SHALL provide a `docs` domain with Maven modules `relayflow-module-docs-api` and `relayflow-module-docs-biz`, and persist docs data in tables prefixed with `doc_`.

#### Scenario: Table naming

- **WHEN** a new docs-domain table is created
- **THEN** its name starts with `doc_`
- **AND** it includes `tenant_id BIGINT NOT NULL`

#### Scenario: Cross-domain isolation

- **WHEN** docs-biz reads or writes persistence
- **THEN** it MUST NOT query `sys_`, `infra_`, `im_`, `task_`, or `cal_` tables via Mapper
- **AND** any needed cross-domain checks MUST go through `*-api`

#### Scenario: Docs Maven module loads

- **WHEN** `relayflow-server` is built with docs-biz on the classpath
- **THEN** module `relayflow-module-docs-api` and `relayflow-module-docs-biz` compile successfully

---

### Requirement: Docs Library V1 schema tables

The system SHALL provide Flyway migrations creating `doc_object` and `doc_library_node` with `doc_` prefix and tenant columns. Library V1 schema MUST NOT require `doc_embed`.

#### Scenario: Core tables exist after migrate

- **WHEN** docs Library V1 schema migrations are applied
- **THEN** tables `doc_object` and `doc_library_node` exist
- **AND** `doc_object` stores `body` as JSONB with a `body_format` column
- **AND** table `doc_embed` is not required

---

### Requirement: Three storage containers product model

The docs product SHALL treat **我的文档库 (Library)**、**云盘 (Drive)**、**知识库 (Wiki)** as three distinct storage containers for document objects. A document object SHALL reside in exactly one container placement at a time (shortcuts are out of scope until a later change).

#### Scenario: Library vs Drive exclusivity

- **WHEN** a document object is placed in the personal Library
- **THEN** it MUST NOT also appear as a Drive folder child solely because of that Library placement

#### Scenario: Future containers reserved

- **WHEN** Library V1 is implemented
- **THEN** the domain model MUST allow later Drive and Wiki placements without changing the document object primary key
- **AND** Drive and Wiki user-visible behavior MAY remain unimplemented stubs in the UI

---

### Requirement: Object type versus block type

The system SHALL distinguish **object types** (what is created in a container, stored on `doc_object.type`) from **block types** (nodes inside a `RICH_DOC` body). Library V1 MUST support object type `RICH_DOC` for Library pages. Folders MUST NOT be modeled as `RICH_DOC` objects (folders belong to Drive).

#### Scenario: Unsupported Library create type

- **WHEN** a client requests Library page creation with an object type other than `RICH_DOC`
- **THEN** the system rejects with `DOC_TYPE_UNSUPPORTED` (or equivalent documented code)

#### Scenario: Block extensions do not invent object types

- **WHEN** a future block such as flowchart is added inside a `RICH_DOC`
- **THEN** it MUST remain object type `RICH_DOC`
- **AND** MUST NOT require a new `doc_object.type` solely for that block

---

### Requirement: Block JSON is the document source of truth

For `RICH_DOC`, the system SHALL persist the editable document body as structured editor JSON (TipTap / ProseMirror document JSON) in `doc_object.body`, with a `body_format` discriminator (V1: `tiptap_json_v1` or equivalent documented value). Markdown, HTML, DOCX, and PDF MUST NOT be the primary stored form of the body.

#### Scenario: Create RICH_DOC with JSON body

- **WHEN** a member creates a new page in their Library
- **THEN** the system creates a `RICH_DOC` with a title and an empty or default TipTap JSON body
- **AND** stores `body_format` identifying the TipTap JSON schema version

#### Scenario: Markdown is not primary storage

- **WHEN** a document is saved after editing
- **THEN** the persisted body MUST be editor JSON
- **AND** MUST NOT replace the primary body with Markdown text

---

### Requirement: TipTap workspace editor

The workspace `RICH_DOC` editor SHALL use TipTap loaded lazily on the `/app/docs` route. V1 MUST support at least: paragraphs, heading levels 1–3, ordered/bullet/task lists, bold/italic, code block, blockquote, horizontal rule, and links. Unsupported insertable block types MAY appear in the UI as disabled placeholders.

#### Scenario: Lazy-loaded editor

- **WHEN** the member navigates to `/app/docs` and selects a document
- **THEN** the TipTap editor renders in the main pane without blocking initial shell paint with the full editor bundle

#### Scenario: Edit and restore

- **WHEN** a logged-in owner edits a document in TipTap and the body is saved
- **THEN** a subsequent open restores the same structured content in TipTap

---

### Requirement: Personal Library page tree

The system SHALL provide each active tenant member an implicit personal Library (no required header row). Library pages form a tree via parent references. V1 MUST allow the owner to create, rename, move within their tree, and soft-delete pages. Only the owner MAY read or write their Library tree and documents.

#### Scenario: Empty library

- **WHEN** a member opens the Library and has never created a page
- **THEN** the tree is empty
- **AND** the system MUST NOT require a pre-inserted root library row

#### Scenario: Create child page

- **WHEN** the owner creates a page under a parent node they own (or at root)
- **THEN** the system creates a library node and linked document object
- **AND** the new node appears under that parent in subsequent tree fetches

#### Scenario: Move within tree

- **WHEN** the owner moves a node to a new parent within their own tree
- **THEN** the node’s parent updates
- **AND** the system MUST reject moves that would create a cycle or reference another user’s node

#### Scenario: Soft delete

- **WHEN** the owner deletes a library node
- **THEN** the node and its document object are soft-deleted
- **AND** they no longer appear in tree or recent lists for that user

#### Scenario: Non-owner forbidden

- **WHEN** a member attempts to read or mutate another user’s Library node or document
- **THEN** the system rejects with a forbidden or not-found business error

---

### Requirement: Personal library App API

The system SHALL expose `/app-api/docs/**` for the authenticated member's personal document library per lane contract: nested library tree, node CRUD with cycle prevention, document get/save with optimistic `contentVersion`, recent list by `last_opened_at`, and Markdown export for `format=md` only.

#### Scenario: Owner-only access

- **WHEN** a user requests a node or document they do not own
- **THEN** the API responds with `DOC_NOT_FOUND` or `DOC_FORBIDDEN`

#### Scenario: Version conflict on body save

- **WHEN** `contentVersion` does not match the stored version
- **THEN** the API responds with `DOC_VERSION_CONFLICT`

---

### Requirement: Document body read and optimistic save

Authenticated owners SHALL read document body JSON and save updates with optimistic concurrency via a monotonically increasing content version.

#### Scenario: Successful save

- **WHEN** the owner PUTs body with the current `contentVersion`
- **THEN** the body is persisted, `contentVersion` increments by 1
- **AND** the response returns the new version

#### Scenario: Version conflict

- **WHEN** the owner PUTs body with a stale `contentVersion`
- **THEN** the system rejects with `DOC_VERSION_CONFLICT` (or equivalent documented code)
- **AND** does not overwrite the stored body

---

### Requirement: Recent documents list

The system SHALL expose a recent list of the current user’s owned documents ordered by last opened time (descending). Opening a document MUST refresh that timestamp.

#### Scenario: Open updates recent

- **WHEN** the owner opens a document they own
- **THEN** `last_opened_at` (or equivalent) is updated
- **AND** subsequent recent list ranks that document near the top

#### Scenario: Recent excludes deleted

- **WHEN** a document is soft-deleted
- **THEN** it MUST NOT appear in the recent list

---

### Requirement: Markdown export adapter

The system SHALL allow the owner to export a `RICH_DOC` as Markdown derived from the stored TipTap JSON. V1 MUST NOT require DOCX or PDF export. Export MAY be lossy for unsupported or future block types; basic V1 blocks SHOULD round-trip to readable Markdown.

#### Scenario: Export Markdown

- **WHEN** the owner requests export with format `md`
- **THEN** the system returns a Markdown document (download or text response as defined by the lane contract)
- **AND** the stored `body` JSON remains unchanged

#### Scenario: Unsupported export format in V1

- **WHEN** the owner requests export with format `docx` or `pdf` in V1
- **THEN** the system rejects with `DOC_EXPORT_FORMAT_UNSUPPORTED` (or equivalent)

---

### Requirement: Heavy embeds deferred

Library V1 MUST NOT persist a `doc_embed` table and MUST NOT ship editable heavy embeds (flowchart, whiteboard, bitable embed, etc.). A later change MAY add `doc_embed` and TipTap nodes that reference embed ids without changing existing `doc_object.id` values.

#### Scenario: No embed table in Library V1 schema

- **WHEN** Library V1 schema migrations are applied
- **THEN** tables `doc_object` and `doc_library_node` exist
- **AND** table `doc_embed` MUST NOT be required for Library V1

---

### Requirement: Workspace docs Library UI

The workspace SHALL provide `/app/docs` with a Library tree, a Recent entry, a TipTap editor for `RICH_DOC`, and a Markdown export action. Drive, Wiki, Shared-with-me, and Starred MAY appear as disabled or “coming soon” placeholders until their changes land. Library MUST NOT require sharing features.

#### Scenario: Create and edit in browser

- **WHEN** a logged-in member visits `/app/docs`, creates a document, edits body, and refreshes
- **THEN** the document remains in the Library tree
- **AND** the saved body is restored in the TipTap editor

#### Scenario: Placeholders for other containers

- **WHEN** the member views `/app/docs` navigation
- **THEN** Library and Recent are usable
- **AND** Drive and Wiki (if shown as placeholders) MUST NOT imply full feature availability

---

### Requirement: Document deep link by docId

The workspace docs page SHALL support opening a document via query parameter `docId` on `/app/docs`.

#### Scenario: Open via query

- **WHEN** a logged-in member visits `/app/docs?docId={objectId}` for an existing document they own
- **THEN** that document is selected when present in the tree
- **AND** its TipTap editor is shown

---

### Requirement: Docs library store uses App API

`useDocsStore` MUST read and write Library data through `web/src/api/app/docs.ts`. It MUST NOT use in-memory temporary Maps or client-only Markdown serialization as the persistence source of truth after integrate.

#### Scenario: First visit loads from API

- **WHEN** the member opens `/app/docs`
- **THEN** the store calls `listLibraryTree` and `listRecentDocuments` to populate the sidebar

#### Scenario: Save body version conflict

- **WHEN** `saveDocumentBody` returns `DOC_VERSION_CONFLICT`
- **THEN** the UI shows an understandable conflict message and does not silently overwrite

#### Scenario: Export Markdown via API

- **WHEN** the member clicks export Markdown
- **THEN** the store calls `exportDocumentMarkdown` and downloads server-generated Markdown

#### Scenario: Tenant switch resets docs state

- **WHEN** the account or tenant is switched
- **THEN** docs store state is cleared and subsequent hydration reloads from the API
