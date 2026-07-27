# docs Specification

## Purpose

云文档域：三大存放容器（我的文档库 / 云盘 / 知识库）边界、文档本体与块模型，工作台 `/app/docs` 我的文档库（Library）与云盘行为，以及对象级分享 / 星标 / 个人最近足迹。知识库能力由后续 change 增量扩展。

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

The system SHALL provide each active tenant member an implicit personal Library (no required header row). Library pages form a tree via parent references. V1 MUST allow the owner to create, rename, move within their tree, and soft-delete pages. Only the owner MAY read or mutate their Library **tree nodes**. Document body access for non-owners MUST follow share ACL (see Document access via owner or share); non-owners MUST NOT appear in or mutate another user's Library tree.

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
- **AND** grantees MUST lose access via share lists for that object

#### Scenario: Non-owner tree forbidden

- **WHEN** a member attempts to read or mutate another user’s Library **node**
- **THEN** the system rejects with a forbidden or not-found business error

#### Scenario: Shared document not in grantee tree

- **WHEN** document O owned by A is shared to B
- **THEN** O MUST NOT appear as a node in B's Library tree solely because of the share

---

### Requirement: Personal library App API

The system SHALL expose `/app-api/docs/**` for the authenticated member's personal document library per lane contract: nested library tree, node CRUD with cycle prevention, document get/save with optimistic `contentVersion`, recent list by `last_opened_at`, and Markdown export for `format=md` only. Document get/save/export authorization MUST allow owners and sufficient share grantees. Library node APIs MUST remain owner-only.

#### Scenario: Owner-only library nodes

- **WHEN** a user requests a library node they do not own
- **THEN** the API responds with `DOC_NOT_FOUND` or `DOC_FORBIDDEN`

#### Scenario: Shared document get allowed

- **WHEN** a user with an active `VIEW` or `EDIT` share requests `GET` document for that object
- **THEN** the API returns the document

#### Scenario: Version conflict on body save

- **WHEN** `contentVersion` does not match the stored version
- **THEN** the API responds with `DOC_VERSION_CONFLICT`

---

### Requirement: Document body read and optimistic save

Authenticated callers with ownership or sufficient share permission SHALL read document body JSON and (for owner or `EDIT` grantees) save updates with optimistic concurrency via a monotonically increasing content version.

#### Scenario: Successful save by owner

- **WHEN** the owner PUTs body with the current `contentVersion`
- **THEN** the body is persisted, `contentVersion` increments by 1
- **AND** the response returns the new version

#### Scenario: Successful save by EDIT grantee

- **WHEN** an `EDIT` grantee PUTs body with the current `contentVersion`
- **THEN** the body is persisted and the version increments

#### Scenario: Version conflict

- **WHEN** a permitted editor PUTs body with a stale `contentVersion`
- **THEN** the system rejects with `DOC_VERSION_CONFLICT` (or equivalent documented code)
- **AND** does not overwrite the stored body

#### Scenario: VIEW cannot save

- **WHEN** a `VIEW` grantee PUTs body
- **THEN** the system rejects with `DOC_FORBIDDEN` or equivalent

---

### Requirement: Recent documents list

The system SHALL expose a recent list backed by the current user's personal visit history (`doc_user_visit`), ordered by `last_opened_at` descending. Successfully opening an accessible document (owned or shared) MUST upsert that user's visit row and refresh `last_opened_at` and `title_snapshot`. Each list item MUST include an `accessState` of at least `OK`, `DELETED`, or `NO_ACCESS`. Tombstone rows (`DELETED` / `NO_ACCESS`) MUST remain listable so the user can see they previously opened the document, MUST NOT return document body, and opening them MUST present a clear unavailable message instead of the editor. Shared-with-me MUST continue to list only active grants (not tombstones).

#### Scenario: Open owned updates visit

- **WHEN** the owner opens a document they own
- **THEN** their visit row is upserted
- **AND** subsequent recent list ranks that document near the top with `accessState=OK`

#### Scenario: Open shared updates visit

- **WHEN** a grantee opens a document shared to them
- **THEN** the grantee's visit row is upserted
- **AND** the document appears in the grantee's recent list with `accessState=OK`
- **AND** the owner's recent ranking MUST NOT change solely because the grantee opened it

#### Scenario: Soft-deleted appears as tombstone

- **WHEN** a document the user previously opened is soft-deleted
- **THEN** the user's recent list MAY still include that visit with `accessState=DELETED` and `title_snapshot`
- **AND** clicking it MUST NOT open the TipTap editor
- **AND** the UI MUST indicate the document no longer exists

#### Scenario: Revoked share appears as tombstone

- **WHEN** a grantee's share is revoked or the grantee leaves after having opened the document
- **THEN** the grantee's recent list MAY still include that visit with `accessState=NO_ACCESS`
- **AND** clicking it MUST NOT return the document body

#### Scenario: Remove from recent only

- **WHEN** the user removes an entry from recent
- **THEN** only the visit row is removed
- **AND** the underlying document object (if any) is unchanged

#### Scenario: Owner may delete from recent menu

- **WHEN** the recent row refers to a document the user owns with `accessState=OK`
- **THEN** the UI MAY offer both「从最近中移除」and「删除」
- **AND**「删除」soft-deletes the document object
- **AND**「从最近中移除」MUST NOT soft-delete the document

#### Scenario: Non-owner cannot delete from recent menu

- **WHEN** the recent row is a shared document (user is grantee only)
- **THEN** the UI MUST NOT offer document delete
- **AND** MUST still offer「从最近中移除」

#### Scenario: Recent list columns

- **WHEN** the member loads the recent list
- **THEN** each item includes at least title (or title snapshot), location label, owner display identity, created time when available, last opened time, and `accessState`

---

### Requirement: Markdown export adapter

The system SHALL allow callers with read access (owner or `VIEW`/`EDIT` share) to export a `RICH_DOC` as Markdown derived from the stored TipTap JSON. V1 MUST NOT require DOCX or PDF export. Export MAY be lossy for unsupported or future block types; basic V1 blocks SHOULD round-trip to readable Markdown.

#### Scenario: Export Markdown

- **WHEN** a reader with access requests export with format `md`
- **THEN** the system returns a Markdown document (download or text response as defined by the lane contract)
- **AND** the stored `body` JSON remains unchanged

#### Scenario: Unsupported export format in V1

- **WHEN** a caller requests export with format `docx` or `pdf` in V1
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

The workspace SHALL provide `/app/docs` with a **主页** entry (Home tabs for recent / shared / starred), a **我的文档库** entry that lists pages in the main pane, a TipTap editor for `RICH_DOC` with back navigation to the prior list context, a Markdown export action, and Drive explorer entry per Drive slide-in requirements. Wiki MAY appear as a disabled or “coming soon” placeholder. Library node management MUST NOT require sharing; sharing remains an orthogonal owner action on documents.

#### Scenario: Create and edit in browser

- **WHEN** a logged-in member visits `/app/docs`, creates a document in the library, edits body, and refreshes
- **THEN** the document remains in the member's library
- **AND** the saved body is restored in the TipTap editor

#### Scenario: Home shared and starred enabled

- **WHEN** the member views `/app/docs` Home tabs
- **THEN**「与我共享」and「星标」are usable from Home (not disabled placeholders)
- **AND** Wiki (if shown as placeholder) MUST NOT imply full feature availability

#### Scenario: Library list not middle-only

- **WHEN** the member selects「我的文档库」
- **THEN** library pages are presented in the main pane as a tree-indented list
- **AND** opening a page shows the editor with a back control

---

### Requirement: Document deep link by docId

The workspace docs page SHALL support opening a document via query parameter `docId` on `/app/docs` for any document the member can access (owned or shared).

#### Scenario: Open owned via query

- **WHEN** a logged-in member visits `/app/docs?docId={objectId}` for an existing document they own
- **THEN** that document is selected when present in the tree
- **AND** its TipTap editor is shown

#### Scenario: Open shared via query

- **WHEN** a logged-in grantee visits `/app/docs?docId={objectId}` for a document shared to them
- **THEN** its TipTap editor is shown
- **AND** the UI MUST NOT require the document to exist in the grantee's Library tree

---

### Requirement: Docs library store uses App API

`useDocsStore` MUST read and write Library, share, shared-with-me, star, recent-visit, and Home-tab data through `web/src/api/app/docs.ts` (and drive APIs via the drive store). It MUST NOT use in-memory temporary Maps, `USE_COLLAB_TEMP` overlays, or client-only Markdown serialization as the persistence source of truth after integrate.

#### Scenario: First visit loads from API

- **WHEN** the member opens `/app/docs`
- **THEN** the store calls APIs needed for Home tabs (recent, shared, starred) and library list/tree hydration

#### Scenario: Share grant via API

- **WHEN** the owner grants VIEW or EDIT in the share dialog
- **THEN** the store calls `createDocumentShare` (or updates an existing grant) and refreshes that object's collaborator list

#### Scenario: Leave share via API

- **WHEN** a grantee leaves a shared document
- **THEN** the store calls `leaveSharedDocument`
- **AND** the document body can no longer be opened
- **AND** recent MAY show a `NO_ACCESS` tombstone

#### Scenario: Star via API

- **WHEN** the member stars or unstars a readable document
- **THEN** the store calls `starDocument` / `unstarDocument` and refreshes the starred list

#### Scenario: Remove from recent via API

- **WHEN** the member removes an entry from recent
- **THEN** the store calls `removeRecentDocument` without deleting the document object

#### Scenario: Save body version conflict

- **WHEN** `saveDocumentBody` returns `DOC_VERSION_CONFLICT`
- **THEN** the UI shows an understandable conflict message and does not silently overwrite

#### Scenario: Export Markdown via API

- **WHEN** the member clicks export Markdown
- **THEN** the store calls `exportDocumentMarkdown` and downloads server-generated Markdown

#### Scenario: Tenant switch resets docs state

- **WHEN** the account or tenant is switched
- **THEN** docs store state is cleared and subsequent hydration reloads from the API

---

### Requirement: Document object share ACL schema

The system SHALL persist per-object share grants in `doc_share` with `doc_` prefix and tenant columns. Each active grant MUST bind exactly one `(tenant_id, object_id, grantee_user_id)` to a permission of `VIEW` or `EDIT`, and MUST record `granted_by_user_id`.

#### Scenario: Tables exist after migrate

- **WHEN** docs share/star schema migrations are applied
- **THEN** table `doc_share` exists
- **AND** an active unique constraint prevents duplicate grants for the same grantee on the same object

#### Scenario: Soft-deleted document hidden from share queries

- **WHEN** a shared `doc_object` is soft-deleted
- **THEN** that object MUST NOT appear in the grantee's shared-with-me list

---

### Requirement: Document star schema

The system SHALL persist personal document stars in `doc_star` keyed by `(tenant_id, user_id, object_id)` with soft delete. Starring MUST NOT grant access by itself.

#### Scenario: Star table exists

- **WHEN** docs share/star schema migrations are applied
- **THEN** table `doc_star` exists
- **AND** a user MAY have at most one active star per object

---

### Requirement: Personal recent visit schema

The system SHALL persist per-user document open history in `doc_user_visit` with `doc_` prefix and tenant columns. Each active row MUST bind `(tenant_id, user_id, object_id)` with `last_opened_at` and a `title_snapshot` captured at open time. Visit rows MUST NOT by themselves grant document access.

#### Scenario: Visit table exists

- **WHEN** docs share/star schema migrations are applied
- **THEN** table `doc_user_visit` exists
- **AND** a user MAY have at most one active visit row per object

---

### Requirement: Share grant and revoke by owner

The document owner SHALL grant, update permission, and revoke share grants for tenant ACTIVE members on owned `RICH_DOC` objects. Non-owners MUST NOT manage grants. Self-grants MUST be rejected. Grantee membership MUST be validated via `system-api` (no direct `sys_*` Mapper access from docs-biz).

#### Scenario: Owner shares VIEW

- **WHEN** the owner grants member B `VIEW` on a `RICH_DOC` they own
- **THEN** a `doc_share` row is persisted
- **AND** B MAY read the document body
- **AND** B MUST NOT save the document body

#### Scenario: Owner upgrades to EDIT

- **WHEN** the owner updates B's grant to `EDIT`
- **THEN** B MAY save the document body subject to optimistic `contentVersion` rules

#### Scenario: Owner revokes

- **WHEN** the owner revokes B's grant
- **THEN** B MUST receive forbidden or not-found on subsequent document reads
- **AND** the object MUST NOT appear in B's shared-with-me list

#### Scenario: Non-owner cannot manage shares

- **WHEN** a non-owner attempts to create or revoke a share on an object
- **THEN** the system rejects with `DOC_FORBIDDEN` or equivalent

#### Scenario: FILE share rejected in V1

- **WHEN** the owner attempts to share a `FILE` object in this change's V1 scope
- **THEN** the system rejects with a documented unsupported error
- **OR** the product UI MUST NOT offer share for `FILE` and the API MUST reject if called

---

### Requirement: Shared-with-me list and leave

The system SHALL expose a shared-with-me list of documents where the current user is an active grantee. The list MUST include enough metadata for the workspace UI (at least object id, title, type, permission, owner, grantor, shared time). A grantee MAY leave (soft-delete their own grant), after which access is removed. Shared-with-me MUST NOT create a Library or Drive placement for the grantee.

#### Scenario: List shared documents

- **WHEN** member B has active grants on documents owned by others
- **THEN** `GET` shared-with-me (per lane contract) returns those documents
- **AND** none of them appear solely because of B's Library tree

#### Scenario: Grantee leaves

- **WHEN** B leaves a shared document
- **THEN** B's grant is soft-deleted
- **AND** B can no longer read the document via share

---

### Requirement: Document access via owner or share

Document body read, optimistic save, and Markdown export MUST authorize the caller if they are the object owner OR hold a sufficient active share (`VIEW` for read/export, `EDIT` for save). Library tree and Drive tree mutate APIs MUST remain owner-only.

#### Scenario: VIEW grantee reads

- **WHEN** a `VIEW` grantee requests document body
- **THEN** the system returns the body
- **AND** a subsequent body save by that grantee is rejected

#### Scenario: EDIT grantee saves with version

- **WHEN** an `EDIT` grantee PUTs body with the current `contentVersion`
- **THEN** the body is persisted and the version increments

#### Scenario: No access

- **WHEN** a member with neither ownership nor share requests a document
- **THEN** the API responds with `DOC_NOT_FOUND` or `DOC_FORBIDDEN`

#### Scenario: Tree remains owner-only

- **WHEN** a grantee requests another user's Library tree or Drive folder mutate API
- **THEN** the system rejects the request

---

### Requirement: Document starring for accessible objects

Authenticated members SHALL star and unstar documents they can currently read (owner or share). The workspace SHALL list the current user's stars, excluding objects that are deleted or no longer accessible. Star state MUST be private to `(tenant_id, user_id)`.

#### Scenario: Star owned document

- **WHEN** the owner stars a document they own
- **THEN** it appears in their starred list

#### Scenario: Star shared document

- **WHEN** a grantee stars a document shared to them
- **THEN** it appears in their starred list
- **AND** the owner's starred list is unaffected

#### Scenario: Inaccessible star hidden

- **WHEN** a user's star remains but their share was revoked
- **THEN** starred list MUST NOT return that object as accessible

---

### Requirement: Docs share bot delivery

After a successful create or permission-upgrade share grant, `docs-biz` SHALL best-effort call `ImBotApi.send` with `botCode=docs-bot` and target scope `SINGLE` for the grantee's tenant membership, including a workspace deep link to `/app/docs?docId={objectId}` (card or text per lane contract). `ImBotApi.send` failures MUST NOT roll back the share grant or fail the share API response. The module MUST NOT use `NotifyInboxApi` or access `im_*` / `infra_notify` mappers.

#### Scenario: Share triggers docs-bot

- **WHEN** the owner successfully shares a document to member B
- **THEN** the system attempts `ImBotApi.send` with `botCode=docs-bot` and SINGLE target for B
- **AND** the share API still returns success if Bot delivery fails

#### Scenario: Cross-domain boundary

- **WHEN** docs-biz delivers a share notification
- **THEN** it MUST call only `im-api` (`ImBotApi`)
- **AND** MUST NOT query `im_*` tables via Mapper

---

### Requirement: Workspace shared and starred navigation

The workspace `/app/docs` **主页** SHALL expose「与我共享」and「星标」as Home tabs (no longer disabled placeholders and no longer required as separate primary sidebar entries). Selecting a tab MUST show the corresponding list in the Home table layout and allow opening documents the user can access. Wiki MAY remain a placeholder. Sharing UI MUST be available to owners of `RICH_DOC` objects. Row and overflow menus MUST be context-appropriate: recent offers remove-from-recent (and document delete only for owners on accessible owned rows); shared-with-me offers leave-share but not owner delete; library and drive keep existing mutations and ADD share/star where the caller is owner or has read access respectively. V1 MUST NOT require create-copy, shortcut, pin, offline, or follow actions.

#### Scenario: Shared tab usable

- **WHEN** a logged-in member opens Home「与我共享」and has at least one active grant
- **THEN** the shared documents are listed and can be opened in the editor

#### Scenario: Starred tab usable

- **WHEN** a logged-in member opens Home「星标」and has stars on accessible documents
- **THEN** those documents are listed and can be opened

#### Scenario: Deep link for shared doc

- **WHEN** a grantee visits `/app/docs?docId={objectId}` for a document shared to them
- **THEN** the TipTap editor shows that document if still accessible

#### Scenario: Recent remove vs delete

- **WHEN** the member uses「从最近中移除」on a recent row
- **THEN** the row disappears from recent
- **AND** if they still own the document it remains in Library or Drive

---

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

Drive listings SHALL return child folders and placed document objects (at least `FILE`; `RICH_DOC` when moved into Drive) with enough metadata for the UI to branch (open vs download) and to render owner identity and modified time columns.

#### Scenario: List folder contents

- **WHEN** the owner lists a Drive folder they own
- **THEN** the response includes child folders and Drive items for objects placed in that folder
- **AND** each item includes enough fields to show an owner chip and modified time in the main-pane table

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

The workspace `/app/docs` navigation SHALL enable the Drive entry using the slide-in folder explorer interaction (middle-pane tree + main-pane listing). Wiki MAY remain a placeholder.

#### Scenario: Drive panel usable

- **WHEN** a logged-in member opens `/app/docs` and enters Drive explorer
- **THEN** they can browse personal folders and items in the main pane listing
- **AND** they can return to docs root navigation

---

### Requirement: Drive folder and item persistence

The system SHALL persist personal drive folders in `doc_drive_folder` and object placements in `doc_drive_item`, and SHALL support `doc_object.type = FILE` with `storage_file_id` referencing an infra file id (logical FK; no DB FK to `infra_file`).

#### Scenario: Schema supports Drive FILE

- **WHEN** Flyway migrations for Drive have been applied
- **THEN** `doc_drive_folder` and `doc_drive_item` exist
- **AND** `doc_object` accepts type `FILE` with optional `storage_file_id`

---

### Requirement: Drive App API for personal folders and FILE registration

The system SHALL expose authenticated `/app-api/docs/drive` endpoints for the owner to manage personal Drive folders, list folder contents, register uploaded infra files as `FILE` objects, and update/delete Drive items, matching the workspace-docs-drive lane contract.

#### Scenario: Owner CRUD and list

- **WHEN** the owner calls Drive folder create/list/update/delete and item mutate APIs for folders they own
- **THEN** the system persists and returns the expected Drive state
- **AND** non-owners are rejected

---

### Requirement: Drive UI uses live App APIs

The workspace Drive panel SHALL load and mutate data via `/app-api/docs/drive` and infra upload/download. Temporary in-store mock Maps MUST be removed.

#### Scenario: Refresh keeps Drive data

- **WHEN** the member creates a folder, uploads a FILE, refreshes `/app/docs` Drive
- **THEN** the folder and file remain
- **AND** download uses the authorized infra download path

---

### Requirement: Docs home with horizontal filter tabs

The workspace `/app/docs` SHALL provide a **主页 (Home)** primary navigation entry. Selecting Home MUST show a main-pane document table filtered by horizontal tabs for at least「最近访问」「与我共享」「星标」. These three collections MUST NOT remain as separate primary sidebar entries once Home is enabled. Each Home tab list MUST include columns for title, location (when applicable), owner chip, created time when available, last visited/opened time when available, and a row actions menu.

#### Scenario: Home tabs switch lists

- **WHEN** a logged-in member opens「主页」and selects「与我共享」
- **THEN** the main pane shows the shared-with-me list in the Home table layout
- **AND** the middle sidebar MUST NOT require a separate primary「与我共享」nav item

#### Scenario: Recent tab columns

- **WHEN** the member views the「最近访问」Home tab
- **THEN** each OK row shows title, location, owner chip, created time when available, last opened time, and row actions

#### Scenario: Starred tab usable from Home

- **WHEN** the member selects the「星标」Home tab and has starred accessible documents
- **THEN** those documents are listed and can be opened in the editor

#### Scenario: Default landing

- **WHEN** a member opens `/app/docs` without a `docId` query
- **THEN** the default primary view is Home with the recent tab selected (unless a later preference overrides it)

#### Scenario: Home upload enters Drive

- **WHEN** the member activates the Home「上传」quick action
- **THEN** the UI enters Drive explorer mode
- **AND** the upload flow is started (file picker or equivalent)
- **AND** MUST NOT only show a toast that tells the user to open Drive manually

---

### Requirement: Library list in main pane

Selecting「我的文档库」MUST present the member's library pages as a **main-pane tree-indented list** (not a flat-only list, and not only as a middle-sidebar tree). The list MUST show hierarchical indentation for parent/child pages, owner chip, created time, last visited/opened time when available, and row actions. When horizontal space is tight, the title column MUST truncate gracefully and indentation depth MUST be capped so other columns remain usable. Opening a row MUST show the TipTap editor with a **back** control that returns to the library list.

#### Scenario: Library content in main pane

- **WHEN** the member selects「我的文档库」and has at least one library page
- **THEN** the main pane lists those pages with owner, created time, and actions
- **AND** child pages are visually indented under their parents
- **AND** the middle sidebar MUST NOT be the sole place that enumerates library page titles

#### Scenario: Narrow width remains usable

- **WHEN** the library list is shown in a narrow main pane
- **THEN** long titles truncate with ellipsis (or equivalent)
- **AND** owner/time/action columns MUST NOT be pushed entirely off-screen by unbounded indentation

#### Scenario: Open and back from library

- **WHEN** the member opens a library page from the list and then activates back
- **THEN** the TipTap editor closes
- **AND** the library list is shown again in the main pane

#### Scenario: Library structure authority unchanged

- **WHEN** the member renames, moves, or soft-deletes a library page from the list row menu
- **THEN** mutations still go through owner-only library node APIs
- **AND** `doc_library_node` remains the structural source of truth

---

### Requirement: Drive slide-in folder explorer

Selecting「云盘」from the docs root navigation MUST enter a **drive explorer** mode: the middle pane is replaced (with a horizontal slide transition) by an IDE-style expandable folder tree, and the main pane shows the current folder's files/folders with owner chip, modified time, and row actions. The cloud-drive root row MUST show a forward affordance on hover indicating that activation enters explorer mode. The member MUST be able to return to the docs root navigation.

#### Scenario: Hover affordance

- **WHEN** the member hovers the「云盘」row in docs root navigation
- **THEN** a forward/enter indicator is visible on that row

#### Scenario: Enter drive explorer

- **WHEN** the member activates「云盘」
- **THEN** the middle pane shows the personal Drive folder tree
- **AND** the main pane shows the listing for the selected folder (or root)
- **AND** a visible control returns to docs root navigation

#### Scenario: Tree selection updates main listing

- **WHEN** the member selects a folder in the drive explorer tree
- **THEN** the main pane listing updates to that folder's children

---

### Requirement: Docs IA list field API

Docs list APIs MUST expose fields required by the home-ia contract for Home / Library / Drive tables including owner identity suitable for owner chips.

#### Scenario: Contract fields present

- **WHEN** client calls listed endpoints in the lane contract
- **THEN** responses include the documented owner and time fields

---

### Requirement: Docs IA integrate

After integrate, `/app/docs` Home / Library / Drive IA MUST work against live App APIs without store-only temporary Maps for this slice.

#### Scenario: Live home recent

- **WHEN** member opens Home recent after integrate
- **THEN** data comes from App API

