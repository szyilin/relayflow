# workspace-rail-order Specification

## Purpose
TBD - created by archiving change workspace-rail-order-v1. Update Purpose after archive.
## Requirements
### Requirement: Product default workspace rail order

The system MUST define a single code-level default order for workspace left-rail entries that applies to every member who has not customized order. The V1 default MUST be: `messages`, `calendar`, `meeting`, `tasks`, `agents`, `docs`, `contacts`. Entries that are not currently registered in the rail MUST NOT appear.

#### Scenario: Fresh user sees product default

- **WHEN** a member has no stored `settings.workspace.railOrder` (or an empty preference row)
- **THEN** the workspace left rail renders in the product default order

#### Scenario: Unknown ids ignored

- **WHEN** a stored `railOrder` contains an id that is not a current rail entry
- **THEN** that id is ignored and remaining known ids keep relative order
- **AND** any registered rail ids missing from the preference are appended in product-default relative order

### Requirement: Per-user drag reorder with drop indicator

The workspace left rail MUST allow the signed-in member to reorder entries by drag-and-drop. While dragging, the UI MUST show a clear insertion indicator (e.g. a line at the drop position). After a successful drop, the new order MUST become the member's effective rail order for the current tenant.

#### Scenario: Drag reorder persists

- **WHEN** a member drags a rail entry to a new position and drops
- **THEN** the rail immediately reflects the new order
- **AND** the order is persisted via the user-preference API as `settings.workspace.railOrder`

#### Scenario: Drop indicator visible during drag

- **WHEN** a member is dragging a rail entry over another entry
- **THEN** an insertion indicator is visible at the intended drop slot

#### Scenario: Click still navigates

- **WHEN** a member clicks a rail entry without completing a drag reorder
- **THEN** navigation to that entry's route still works

