## ADDED Requirements

### Requirement: Personal library App API

The system SHALL expose `/app-api/docs/**` for the authenticated member's personal document library per lane contract: nested library tree, node CRUD with cycle prevention, document get/save with optimistic `contentVersion`, recent list by `last_opened_at`, and Markdown export for `format=md` only.

#### Scenario: Owner-only access

- **WHEN** a user requests a node or document they do not own
- **THEN** the API responds with `DOC_NOT_FOUND` or `DOC_FORBIDDEN`

#### Scenario: Version conflict on body save

- **WHEN** `contentVersion` does not match the stored version
- **THEN** the API responds with `DOC_VERSION_CONFLICT`
