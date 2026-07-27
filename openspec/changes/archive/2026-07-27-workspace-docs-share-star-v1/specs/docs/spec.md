## ADDED Requirements

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

The workspace `/app/docs` navigation SHALL enable「与我共享」and「星标」entries (no longer disabled placeholders). Selecting them MUST show the corresponding lists and allow opening documents the user can access. Wiki MAY remain a placeholder. Sharing UI MUST be available to owners of `RICH_DOC` objects. Row and overflow menus MUST be panel-appropriate: recent offers remove-from-recent (and document delete only for owners on accessible owned rows); shared-with-me offers leave-share but not owner delete; library and drive keep existing tree/browser mutations and ADD share/star where the caller is owner or has read access respectively. V1 MUST NOT require create-copy, shortcut, pin, offline, or follow actions.

#### Scenario: Shared panel usable

- **WHEN** a logged-in member opens「与我共享」and has at least one active grant
- **THEN** the shared documents are listed and can be opened in the editor

#### Scenario: Starred panel usable

- **WHEN** a logged-in member opens「星标」and has stars on accessible documents
- **THEN** those documents are listed and can be opened

#### Scenario: Deep link for shared doc

- **WHEN** a grantee visits `/app/docs?docId={objectId}` for a document shared to them
- **THEN** the TipTap editor shows that document if still accessible

#### Scenario: Recent remove vs delete

- **WHEN** the member uses「从最近中移除」on a recent row
- **THEN** the row disappears from recent
- **AND** if they still own the document it remains in Library or Drive

---

## MODIFIED Requirements

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

### Requirement: Workspace docs Library UI

The workspace SHALL provide `/app/docs` with a Library tree, a Recent entry, a TipTap editor for `RICH_DOC`, a Markdown export action, and enabled「与我共享」and「星标」navigation. Drive remains available per Drive requirements. Wiki MAY appear as a disabled or “coming soon” placeholder. Library tree management MUST NOT require sharing; sharing is an orthogonal owner action on documents.

#### Scenario: Create and edit in browser

- **WHEN** a logged-in member visits `/app/docs`, creates a document, edits body, and refreshes
- **THEN** the document remains in the Library tree
- **AND** the saved body is restored in the TipTap editor

#### Scenario: Shared and starred enabled

- **WHEN** the member views `/app/docs` navigation after this change
- **THEN**「与我共享」and「星标」are usable (not disabled placeholders)
- **AND** Wiki (if shown as placeholder) MUST NOT imply full feature availability

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
