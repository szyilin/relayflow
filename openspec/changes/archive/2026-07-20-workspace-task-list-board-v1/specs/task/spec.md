## ADDED Requirements

### Requirement: Task list container

The system SHALL provide task lists (`task_list`) within the current tenant so members can group root tasks into named containers. Creating a list MUST insert the creator as an `OWNER` member in the same transaction. Lists MUST use table prefix `task_` and include `tenant_id`.

#### Scenario: Create list

- **WHEN** an authenticated workspace member creates a list with a non-empty name
- **THEN** the system persists a `task_list` row scoped to the JWT tenant
- **AND** inserts a `task_list_member` row for the creator with role `OWNER`
- **AND** the list appears in that user's list navigation

#### Scenario: Rename list

- **WHEN** an `OWNER` updates the list name to a non-empty value
- **THEN** the name persists
- **AND** non-owners attempting rename are rejected with a business forbidden error

#### Scenario: Archive list

- **WHEN** an `OWNER` archives a list
- **THEN** the list MUST NOT appear in the default active list navigation
- **AND** tasks that belonged to the list MUST NOT be deleted solely because of archive
- **AND** those tasks MUST still appear in personal views such as「我负责的」when assignee matches

### Requirement: Task list membership

The system SHALL persist list members in `task_list_member` with roles `OWNER`, `EDITOR`, or `VIEWER`. Inviting a member MUST validate the target user is an ACTIVE member of the current tenant via `system-api`. V1 invite and role changes MUST be restricted to `OWNER`. Duplicate invite for the same user MUST be idempotent or rejected with a clear business error (implementation MUST pick one and document in contract).

#### Scenario: Owner invites editor

- **WHEN** an `OWNER` invites an active tenant member as `EDITOR`
- **THEN** the member can see the list and mutate list tasks per list task permission rules
- **AND** the invitee appears in the list member API

#### Scenario: Reject non-member invite

- **WHEN** an `OWNER` invites a user who is not an ACTIVE member of the tenant
- **THEN** the system rejects the invite

#### Scenario: Viewer cannot rename list

- **WHEN** a `VIEWER` attempts to rename or archive the list or invite members
- **THEN** the system rejects the mutation

#### Scenario: Non-member cannot access list

- **WHEN** a user who is not a list member requests list detail, list tasks, or list member APIs for that list
- **THEN** the system rejects with a business forbidden or not-found error that does not leak list existence beyond tenant norms documented in contract

### Requirement: Optional task list affiliation

The system SHALL allow `task_item.list_id` to be null. Creating a task in a list context MUST set `list_id` to that list. Personal「我负责的」root listing MUST continue to return tasks where `assignee_id` equals the current user regardless of `list_id`. Listing tasks by `listId` MUST return root tasks visible to list members (not only the current assignee).

#### Scenario: Create task in list

- **WHEN** an authorized list member creates a root task with `listId` set to a list they can edit
- **THEN** the task is stored with that `list_id`
- **AND** it appears in that list's task page API
- **AND** if the creator is also assignee, it appears in「我负责的」

#### Scenario: Create personal task without list

- **WHEN** a user creates a task without `listId`
- **THEN** `list_id` is null
- **AND** existing personal CRUD semantics remain unchanged

#### Scenario: List page filters by listId

- **WHEN** a list member requests task page with `listId`
- **THEN** the system returns root tasks for that list in the current tenant
- **AND** MUST NOT return tasks from other lists

#### Scenario: Subtask inherits list

- **WHEN** an authorized user creates a subtask under a parent that has a non-null `list_id`
- **THEN** the subtask MUST use the same `list_id`
- **AND** V1 MUST NOT allow moving a subtask to a different list than its parent

### Requirement: Three-state task status for board columns

The system SHALL support `task_item.status` values `TODO`, `IN_PROGRESS`, and `DONE`. Board columns in a list context MUST map one-to-one to these statuses. Completing a task via checkbox or complete action MUST set `DONE`. Un-completing a `DONE` task MUST set `TODO` (not `IN_PROGRESS`). Existing due Bot and calendar projection rules based on `TODO` vs `DONE` MUST treat `IN_PROGRESS` like an incomplete task (eligible for due reminders and due-range projection when other criteria match).

#### Scenario: Move to in progress

- **WHEN** an authorized user sets a task status to `IN_PROGRESS`
- **THEN** the value persists
- **AND** the task appears in the board's in-progress column for its list

#### Scenario: Complete clears in progress

- **WHEN** an authorized user marks a task done
- **THEN** status becomes `DONE`
- **AND** due Bot delivery rules that skip `DONE` still skip this task

#### Scenario: Uncomplete returns to TODO

- **WHEN** an authorized user marks a `DONE` task as not done
- **THEN** status becomes `TODO`

#### Scenario: IN_PROGRESS in due-range

- **WHEN** a task is `IN_PROGRESS` with `due_time` in the requested window and assignee is the current user
- **THEN** `GET /app-api/task/item/due-range` MUST include it
- **AND** MUST NOT include `DONE` tasks

### Requirement: List board view and drag reorder

Within a list context, the `/app/tasks` UI SHALL provide a board view with three columns (`TODO`, `IN_PROGRESS`, `DONE`). Dragging a card between columns MUST persist the new status. Dragging within a column MUST persist relative order via `board_rank` (or equivalent documented field). Opening a card MUST reuse the existing task detail panel / `?taskId=` deep link behavior. Personal nav views (mine / following / activity / etc.) MUST NOT require a full board implementation in V1; they MAY hide the board tab or show guidance to open a list.

#### Scenario: Drag across columns

- **WHEN** an authorized editor or owner drags a root task from TODO to IN_PROGRESS on the list board
- **THEN** the task status becomes `IN_PROGRESS`
- **AND** a refresh shows the card in the in-progress column

#### Scenario: Drag within column

- **WHEN** an authorized user reorders two TODO cards in the same list board column
- **THEN** the new order persists across refresh

#### Scenario: Viewer cannot drag

- **WHEN** a `VIEWER` attempts a board move or status change
- **THEN** the system rejects the mutation

#### Scenario: Deep link to list and task

- **WHEN** a user opens `/app/tasks?listId={listId}&taskId={taskId}` and is a member of that list
- **THEN** the UI shows the list context and opens the detail for that task
- **AND** unauthorized users MUST NOT see list tasks

### Requirement: Workspace task navigation includes lists

The `/app/tasks` left navigation SHALL show an active-lists section for lists where the current user is a member, plus an entry to create a list. Selecting a list MUST switch the center panel to that list's list or board view. Existing entries「我负责的」「我创建的」「已完成」「我关注的」「动态」MUST remain available.

#### Scenario: Switch to list

- **WHEN** the user selects a list in the left nav
- **THEN** the center panel loads that list's tasks
- **AND** list/board toggle is available for that context

#### Scenario: Create list from nav

- **WHEN** the user creates a list from the tasks page
- **THEN** the new list appears in the left nav without a full page reload
- **AND** the UI can select the new list as current context

## MODIFIED Requirements

### Requirement: Personal task CRUD for workspace

The system SHALL allow authenticated workspace members to manage task items assigned to themselves within the current tenant. Task items MUST support optional `start_time`, `due_time`, `description`, `remind_before_minutes`, and optional `list_id` in addition to title and status. Status MUST be one of `TODO`, `IN_PROGRESS`, or `DONE` (default `TODO` on create). List endpoints for「我负责的」MUST return only **root** tasks (`parent_id` is null) unless a request explicitly asks for children.

#### Scenario: List my tasks

- **WHEN** a user requests `GET /app-api/task/item/page` without a list filter
- **THEN** the system returns root tasks where `assignee_id` equals the current user and `tenant_id` equals the JWT tenant
- **AND** supports optional filter by `status` (`TODO`, `IN_PROGRESS`, or `DONE`)

#### Scenario: Create task

- **WHEN** a user posts `POST /app-api/task/item/create` with a non-empty title
- **THEN** the system creates a `task_item` with `assignee_id` and `creator_id` set to the current user (unless assignee is explicitly set and authorized in a later collab requirement)
- **AND** default `status` is `TODO`
- **AND** `parent_id` is null for top-level creates
- **AND** `list_id` is null unless a permitted `listId` is provided

#### Scenario: Toggle task done

- **WHEN** a user posts `PUT /app-api/task/item/toggle-done` for a task they may complete
- **THEN** the system sets `status` to `DONE` when done is true
- **AND** sets `status` to `TODO` when done is false

#### Scenario: Forbidden on others tasks

- **WHEN** a user attempts to update or delete a task where they are neither assignee nor otherwise authorized (e.g. follower read-only, or list EDITOR/OWNER when the task belongs to that list)
- **THEN** the system rejects mutating operations with business error `TASK_FORBIDDEN`
