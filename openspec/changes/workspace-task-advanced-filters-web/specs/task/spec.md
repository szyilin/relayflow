## MODIFIED Requirements

### Requirement: Quick access and personal view contexts

The system SHALL expose workspace task navigation contexts that are **query presets**, not list containers: at minimum「我负责的」(`MINE`),「我关注的」(`FOLLOWING`),「全部任务」(`ALL`),「我创建的」(`CREATED`),「我分配的」(`ASSIGNED_BY_ME`), and「已完成」(`COMPLETED`), in addition to real task lists (`LIST`). Opening a quick-access context MUST initialize that context's ViewConfig filters from a documented default filter seed when no saved config exists (and MAY apply a default sort). The seed clauses MUST be visible and editable in the view toolbar filter UI. After the user edits filters, the system MUST keep the user on the same navigation context and MUST query using the saved filters (MUST NOT silently re-overwrite with the seed on each visit). Users MAY add, change, or remove filter clauses via the toolbar. Resetting the view config MUST restore the documented seed for that context. These contexts MUST NOT define list membership, list-scoped custom field schemas, or shared list groups.

Default seeds (V1) expressed as FilterClause rows:

- `MINE`: `assigneeId CONTAINS` current user AND `status IN (TODO, IN_PROGRESS)`
- `FOLLOWING`: no filter seed (membership via following API)
- `CREATED`: `creatorId EQ` current user
- `ASSIGNED_BY_ME`: `assignerId EQ` current user AND `assigneeId NOT_CONTAINS` current user
- `COMPLETED`: `status IN (DONE)`
- `ALL`: no filter seed; visibility union per API contract (MUST NOT mean all tasks in the tenant)
- `LIST`: no filter seed beyond list membership

#### Scenario: Open created-by-me shows seed in filter UI

- **WHEN** the user opens「我创建的」with no prior saved filters for that context
- **THEN** the filter UI shows a clause equivalent to creator equals the current user
- **AND** the result set is rooted in tasks matching those filters
- **AND** the context MUST NOT require a `listId`

#### Scenario: Edit filters stays on quick-access entry

- **WHEN** the user is on「我创建的」and changes or clears filter clauses then saves
- **THEN** the navigation context remains「我创建的」
- **AND** subsequent loads of that context use the saved filters rather than re-applying only the seed

#### Scenario: Open assigned-by-me

- **WHEN** the user opens「我分配的」with default seed
- **THEN** the filter UI shows assigner equals current user and assignee does not contain current user
- **AND** the result set matches those clauses

#### Scenario: Open completed

- **WHEN** the user opens「已完成」with default seed
- **THEN** the filter UI shows status includes done
- **AND** the result set is tasks in the done state within the visibility rules of that context
- **AND** MAY default sort by completion time

### Requirement: Task view context and persisted view config

For each view context (`MINE`, `FOLLOWING`, `ALL`, `CREATED`, `ASSIGNED_BY_ME`, `COMPLETED`, and each `LIST`), the system MUST persist a view configuration that includes at least: `displayMode` (`LIST` or `BOARD`), `groupBy` mode (none, field, personal-custom for `MINE`, or list-group for `LIST`), sort mode (including manual/drag order), AND-combined filter clauses, and visible field keys. List and board presentations of the same context MUST share `groupBy`, sort, and filters.

Filter clauses MUST support at least system fields `status`, `assigneeId`, `creatorId`, `assignerId`, `dueTime`, `createTime`, and `title`, each with field-appropriate operators (e.g. `IN` / `EQ` for status; `CONTAINS` / `NOT_CONTAINS` / `EQ` / `NE` / `IS_EMPTY` / `IS_NOT_EMPTY` for people; relative/absolute compare ops for times; `CONTAINS` / `EQ` for title). The toolbar filter UI MUST present each clause as field + operator + value and allow add/remove. Until server-side filter application is integrated, the workspace UI MAY apply filters client-side to the loaded page and MUST still persist the full clause list in ViewConfig.

For non-`LIST` contexts, view config MUST be private to `(tenant_id, user_id, contextType)` — only that user MAY read or update it. For `LIST`, the system MUST store a list-scoped shared default readable by list members; create/update of the shared config MUST be restricted to `OWNER` or `EDITOR`. VIEWER MUST NOT persist shared list config (session-local UI tweaks MAY exist without server persistence).

#### Scenario: Save personal mine view

- **WHEN** the current user updates view config for「我负责的」
- **THEN** the system persists it for that user only
- **AND** another user MUST NOT receive that config when loading their own「我负责的」

#### Scenario: Save list shared view

- **WHEN** an `OWNER` or `EDITOR` saves view config for a list context
- **THEN** other list members loading that list receive the updated shared config
- **AND** a `VIEWER` attempting to save shared config is rejected

#### Scenario: List and board share groupBy

- **WHEN** a context has `groupBy` set and the user switches `displayMode` between list and board
- **THEN** both modes present partitions derived from the same `groupBy`
- **AND** filters and sort remain unchanged by the mode switch alone

#### Scenario: Toolbar shows multi-field filters

- **WHEN** the user opens the filter control on a task view
- **THEN** they can add a clause choosing a supported field, operator, and value
- **AND** the active filter count reflects the number of clauses
- **AND** clearing filters sets an empty list without leaving the current context
