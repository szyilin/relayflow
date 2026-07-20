## ADDED Requirements

### Requirement: Quick access and personal view contexts

The system SHALL expose workspace task navigation contexts that are **query presets**, not list containers: at minimum「我负责的」(`MINE`),「我关注的」(`FOLLOWING`),「全部任务」(`ALL`),「我创建的」(`CREATED`),「我分配的」(`ASSIGNED_BY_ME`), and「已完成」(`COMPLETED`), in addition to real task lists (`LIST`). Opening a quick-access context MUST apply a documented default filter seed (and MAY apply a default sort). Users MAY add further filter clauses via the toolbar. These contexts MUST NOT define list membership, list-scoped custom field schemas, or shared list groups.

Default seeds (V1):

- `MINE`: assignee set contains the current user
- `FOLLOWING`: current user follows the task
- `CREATED`: creator is the current user
- `ASSIGNED_BY_ME`: assigner is the current user AND assignee set does not contain the current user
- `COMPLETED`: task completion/status is done
- `ALL`: tasks visible to the current user under the visibility union documented in the API contract (MUST NOT mean all tasks in the tenant)

#### Scenario: Open created-by-me

- **WHEN** the user opens「我创建的」
- **THEN** the result set is rooted in tasks whose creator is the current user (plus any extra filters the user saved on that context)
- **AND** the context MUST NOT require a `listId`

#### Scenario: Open assigned-by-me

- **WHEN** the user opens「我分配的」
- **THEN** the system returns root tasks where assigner is the current user and the current user is not in the assignee set

#### Scenario: Open completed

- **WHEN** the user opens「已完成」
- **THEN** the system returns tasks in the done completion state for the visibility rules of that context
- **AND** MAY default sort by completion time

### Requirement: Task view context and persisted view config

For each view context (`MINE`, `FOLLOWING`, `ALL`, `CREATED`, `ASSIGNED_BY_ME`, `COMPLETED`, and each `LIST`), the system MUST persist a view configuration that includes at least: `displayMode` (`LIST` or `BOARD`), `groupBy` mode (none, field, personal-custom for `MINE`, or list-group for `LIST`), sort mode (including manual/drag order), AND-combined filter clauses (layered on the context seed), and visible field keys. List and board presentations of the same context MUST share `groupBy`, sort, and filters.

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

### Requirement: Group by field and empty bucket

When `groupBy` mode is field-based, the system SHALL partition root tasks in that context by the field's value. Tasks with empty/missing values MUST appear under「无分组」(or an equivalent stable empty key in the contract). Dragging between partitions MUST persist an update to that field per target bucket. Clearing into「无分组」MUST clear the field when null is allowed; completion/status MUST follow explicit mapping rules (MUST NOT invent invalid status values).

V1 field-group delivery MUST support system fields first (at least completion/status and assignees; due-time bucketing MAY follow). Fixed boards that hard-code only `TODO` / `IN_PROGRESS` / `DONE` columns MUST NOT remain the long-term board model once field grouping is integrated — status enum values MAY remain on `task_item`.

#### Scenario: Board columns from status groupBy

- **WHEN** a context sets field `groupBy` to status/completion
- **THEN** the board shows columns for the configured/present values
- **AND** dragging a card into another status column persists the new status

#### Scenario: Empty value bucket

- **WHEN** field `groupBy` is set and some tasks leave the field empty
- **THEN** those tasks appear under「无分组」
- **AND** dragging into「无分组」clears that field when null is allowed

#### Scenario: List groups when field groupBy absent

- **WHEN** a list context has no field `groupBy` and has list-local groups
- **THEN** list/board partitions MUST use those list-local groups (including the default group)

### Requirement: Personal custom groups for mine inbox

The system SHALL allow each user to maintain private named groups for their「我负责的」inbox (personal custom groups), including exactly one default group per user. Placement of a task into a personal group MUST be scoped to `(tenant_id, user_id)` and MUST NOT affect any other user's mine inbox grouping. When the user's `MINE` view config selects personal-custom grouping, list/board partitions MUST use these personal groups. Creating/renaming/reordering/deleting non-default personal groups MUST be limited to the owning user; deleting a non-default group MUST move its tasks to that user's default group without deleting tasks.

#### Scenario: Personal group not visible to others

- **WHEN** user A places a shared task into A's personal group G
- **THEN** user B viewing「我负责的」MUST NOT see group G
- **AND** B's partitions follow B's own personal groups or field groupBy

#### Scenario: Delete personal group

- **WHEN** the user deletes a non-default personal group that contains tasks
- **THEN** those tasks move to the user's default personal group
- **AND** the tasks remain in「我负责的」if the user is still an assignee

### Requirement: Task assigner field

The system SHALL persist an **assigner** (分配人) for assignment operations so that「我分配的」can be queried. When an authorized user changes the assignee set such that the assigner is recorded per product rules, `assigner` MUST be set to that user (validated via `system-api` as an active tenant member when applicable).「我分配的」MUST return root tasks where assigner is the current user and the current user is not in the assignee set.

#### Scenario: Assign to others records assigner

- **WHEN** user A sets assignees to only user B (A not in the set)
- **THEN** assigner becomes A
- **AND** the task appears in A's「我分配的」and in B's「我负责的」
- **AND** the task MUST NOT appear in A's「我负责的」solely due to having assigned it

#### Scenario: Assigned-by-me excludes self-assignee

- **WHEN** user A is both assigner and still in the assignee set
- **THEN** the task MUST NOT appear in A's「我分配的」under the default seed

### Requirement: Multi-assignee and mine inbox membership

The system SHALL allow a task to have zero or more assignees (multi-select). A root task MUST appear in a user's「我负责的」when and only when that user is among the assignees (and tenant matches). Creating a task MUST allow setting the initial assignee set; default MUST include the current user unless the API explicitly sets otherwise. Single-column `assignee_id` MAY exist only as a migration projection and MUST NOT remain the sole source of truth after multi-assignee integrate.

#### Scenario: Mine includes me among many

- **WHEN** a root task has assignees A and B
- **THEN** both A and B see it in「我负责的」
- **AND** user C who is only the creator and not an assignee MUST NOT see it in「我负责的」solely due to creating it

#### Scenario: Remove assignee drops from mine

- **WHEN** user A is removed from a task's assignee set
- **THEN** the task disappears from A's「我负责的」root list
- **AND** remaining assignees still see it

### Requirement: Multi-list task membership

The system SHALL allow a root task to belong to zero or more lists via an explicit membership relation (not a single exclusive `list_id` as the long-term model). Listing by `listId` MUST return root tasks that are members of that list. Adding/removing list membership MUST NOT delete the task row. Task detail MUST expose memberships and MUST allow authorized users to set the list-local group for each membership when list groups exist. Subtasks MUST follow parent list memberships in V1.

#### Scenario: Task in two lists

- **WHEN** a root task is a member of list L1 and list L2
- **THEN** it appears in both lists' task queries for authorized members
- **AND** removing it from L1 leaves it on L2 and does not delete the task

#### Scenario: Personal mine ignores list membership for inclusion

- **WHEN** a task has assignees including the current user and zero list memberships
- **THEN** it still appears in「我负责的」

### Requirement: List-local groups

Within a list, the system SHALL support named groups including exactly one default group per list. New tasks joining a list MUST land in the default group unless another group is specified. Users with edit rights MAY create, rename, reorder, and archive/delete non-default groups per contract rules; deleting a group MUST move its tasks to the default group (MUST NOT delete tasks). List-local groups apply when presenting a list context without field `groupBy`; they remain editable as per-list placement even when field `groupBy` is active.

#### Scenario: Default group on join

- **WHEN** a task is added to a list without specifying a group
- **THEN** it is placed in that list's default group

#### Scenario: Delete custom group

- **WHEN** an authorized editor deletes a non-default group that contains tasks
- **THEN** those tasks move to the default group
- **AND** the tasks remain list members

## MODIFIED Requirements

### Requirement: Personal task CRUD for workspace

The system SHALL allow authenticated workspace members to manage task items for which they are authorized within the current tenant. Task items MUST support optional `start_time`, `due_time`, `description`, and `remind_before_minutes` in addition to title and status. List endpoints for「我负责的」MUST return only **root** tasks (`parent_id` is null) unless a request explicitly asks for children. Inclusion in「我负责的」MUST be based on the current user being in the task's **assignee set** (multi-assignee), not solely a single `assignee_id` column after multi-assignee delivery.

#### Scenario: List my tasks

- **WHEN** a user requests the mine-context task page API
- **THEN** the system returns root tasks where the assignee set contains the current user and `tenant_id` equals the JWT tenant
- **AND** supports optional filter by status/completion as applicable

#### Scenario: Create task

- **WHEN** a user posts create with a non-empty title
- **THEN** the system creates a `task_item` with `creator_id` set to the current user
- **AND** the default assignee set includes the current user unless the request explicitly sets another authorized assignee set
- **AND** default `status` is `TODO`
- **AND** `parent_id` is null for top-level creates

#### Scenario: Toggle task done

- **WHEN** a user posts toggle-done for a task they may complete
- **THEN** the system sets status to `DONE` or `TODO` according to the request

#### Scenario: Forbidden on others tasks

- **WHEN** a user attempts to update or delete a task where they are neither an assignee nor otherwise authorized (e.g. follower read-only, list editor)
- **THEN** the system rejects mutating operations with business error `TASK_FORBIDDEN`

### Requirement: Task assignment

The system SHALL allow authorized users to replace or edit the task **assignee set** with one or more active members of the current tenant (validated via `system-api`). After the set changes, the task MUST appear in「我负责的」for each user in the set (if root) and MUST NOT appear for users removed from the set solely for that reason. The system MUST record **assigner** according to the assigner-field requirement when the assignment qualifies. Delivery via `task-bot` MUST be best-effort to newly added assignees and MUST NOT fail the assign API.

#### Scenario: Assign to members

- **WHEN** an authorized user sets assignees to members B and C in the same tenant
- **THEN** the assignee set becomes {B, C}
- **AND** the system attempts best-effort `ImBotApi.send` with `botCode=task-bot` to newly added assignees (SINGLE each) with deep link to the task

#### Scenario: Assign rejects non-member

- **WHEN** any target user in the assignee set is not an active member of the tenant
- **THEN** the system rejects the assign
