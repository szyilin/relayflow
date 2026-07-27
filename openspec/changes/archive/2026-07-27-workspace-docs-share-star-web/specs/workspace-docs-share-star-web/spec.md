## ADDED Requirements

### Requirement: Workspace share-star web demo

The workspace `/app/docs` SHALL enable「与我共享」and「星标」, upgrade「最近」with location/owner/created/last-visited columns and `accessState`, and provide panel-appropriate row menus including remove-from-recent and owner delete. Until API integrate, share/star/visit demo data MAY live in the docs store only.

#### Scenario: Shared and starred enabled

- **WHEN** a logged-in member opens `/app/docs`
- **THEN**「与我共享」and「星标」are usable (not disabled placeholders)

#### Scenario: Remove from recent

- **WHEN** the member chooses「从最近中移除」
- **THEN** the row leaves the recent list without deleting the document object when it is owned
