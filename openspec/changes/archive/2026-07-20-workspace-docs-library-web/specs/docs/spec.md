## ADDED Requirements

### Requirement: Workspace docs library tree UI

The workspace `/app/docs` page SHALL show the current user's personal Library as an interactive page tree with actions to create, rename, move within the tree, and delete pages. Until the docs API is integrated, tree data MAY be held in a Pinia store with seed data; pages MUST NOT import from a persistent `mocks/` module.

#### Scenario: Create page in tree

- **WHEN** a logged-in member creates a new page from the Library tree
- **THEN** a new node appears in the tree with a default title
- **AND** selecting it opens the TipTap editor for that document

#### Scenario: Tree CRUD on local data

- **WHEN** the member renames, moves, or deletes a page before API integration
- **THEN** the tree UI updates immediately to reflect the action

---

### Requirement: Workspace docs recent entry

The `/app/docs` navigation SHALL include a Recent section listing recently opened owned documents ordered by last opened time. Drive, Wiki, Shared-with-me, and Starred entries MAY appear as disabled or "coming soon" placeholders and MUST NOT imply full feature availability in this lane.

#### Scenario: Recent lists opened docs

- **WHEN** the member opens a document from the Library tree
- **THEN** that document appears in the Recent list for the session

#### Scenario: Other containers are placeholders

- **WHEN** the member views Drive, Wiki, Shared, or Starred nav items
- **THEN** those items are disabled or marked as not yet available
- **AND** Library and Recent remain usable

---

### Requirement: TipTap editor on workspace docs

The workspace document editor SHALL use TipTap loaded lazily on the `/app/docs` route. V1 MUST support paragraphs, heading levels 1–3, ordered/bullet/task lists, bold/italic, code block, blockquote, horizontal rule, and links. Unsupported block types MAY appear disabled in an insert menu.

#### Scenario: Lazy-loaded editor

- **WHEN** the member navigates to `/app/docs` and selects a document
- **THEN** the TipTap editor renders in the main pane without blocking initial shell paint with the full editor bundle

#### Scenario: Edit restores content

- **WHEN** the member edits body text and the store persists the TipTap JSON
- **THEN** re-selecting the same document restores the edited content in TipTap

---

### Requirement: Document deep link by docId

The workspace docs page SHALL support opening a document via query parameter `docId` on `/app/docs`.

#### Scenario: Open via query

- **WHEN** a logged-in member visits `/app/docs?docId={objectId}` for an existing local or API-backed document
- **THEN** that document is selected in the tree (if visible)
- **AND** its TipTap editor is shown

---

### Requirement: Markdown export entry in workspace docs UI

The workspace docs editor SHALL expose a user-visible action labeled to indicate export as Markdown (not primary storage). Until API integration, the action MAY download Markdown produced by client-side serialization from TipTap JSON; after integration it MUST call the lane contract export endpoint.

#### Scenario: Export downloads markdown file

- **WHEN** the member clicks the Markdown export action on an open document
- **THEN** the browser downloads a `.md` file derived from the current document content
- **AND** the in-editor TipTap JSON body is unchanged
