## ADDED Requirements

### Requirement: Workspace Drive UI entry enabled

The workspace `/app/docs` navigation SHALL enable the Drive entry so members can open「我的文件夹」browse UI. Shared and Wiki MAY remain placeholders.

#### Scenario: Open Drive panel

- **WHEN** a logged-in member opens `/app/docs` and selects Drive
- **THEN** the Drive browser is shown with breadcrumb, create-folder, and upload affordances
- **AND** Shared / Wiki entries MUST NOT imply full availability if still placeholders

#### Scenario: Browse and create folder (client mock until integrate)

- **WHEN** the member creates a folder or navigates into a folder in the Drive panel before APIs are integrated
- **THEN** the UI updates from temporary store state so the flow is demonstrable
- **AND** temporary local data MUST be removed in the integrate slice
