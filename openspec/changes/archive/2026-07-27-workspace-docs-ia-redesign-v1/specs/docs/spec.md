## ADDED Requirements

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

## MODIFIED Requirements

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

### Requirement: Workspace Drive UI entry

The workspace `/app/docs` navigation SHALL enable the Drive entry using the slide-in folder explorer interaction (middle-pane tree + main-pane listing). Wiki MAY remain a placeholder.

#### Scenario: Drive panel usable

- **WHEN** a logged-in member opens `/app/docs` and enters Drive explorer
- **THEN** they can browse personal folders and items in the main pane listing
- **AND** they can return to docs root navigation

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

### Requirement: Drive listing mixes folders and placed objects

Drive listings SHALL return child folders and placed document objects (at least `FILE`; `RICH_DOC` when moved into Drive) with enough metadata for the UI to branch (open vs download) and to render owner identity and modified time columns.

#### Scenario: List folder contents

- **WHEN** the owner lists a Drive folder they own
- **THEN** the response includes child folders and Drive items for objects placed in that folder
- **AND** each item includes enough fields to show an owner chip and modified time in the main-pane table
