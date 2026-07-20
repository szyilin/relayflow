# task Specification

## Purpose

定义员工工作台协作任务（`task_item`）的领域模块、API 与 `/app/tasks` 页面对接行为。含详情字段、子任务、关注/评论/动态/多负责人指派、快速访问视图、ViewConfig、字段/个人/清单分组、多清单归属与清单自定义字段。
## Requirements
### Requirement: Task item domain module

The system SHALL provide a `task` domain with Maven modules `relayflow-module-task-api` and `relayflow-module-task-biz`, and persist task data in tables prefixed with `task_`.

#### Scenario: Task table naming

- **WHEN** a new task table is created
- **THEN** its name starts with `task_`
- **AND** it includes `tenant_id BIGINT NOT NULL`

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

### Requirement: Workspace tasks page integration

The `/app/tasks` page SHALL use the task API via store layer instead of inline mock data. The page SHALL provide a list of root tasks and a **detail panel** opened via selection or `?taskId=` deep link.

#### Scenario: Create task from UI

- **WHEN** a user creates a task from `/app/tasks`
- **THEN** the task appears in the list without a full page reload
- **AND** persists after refresh

#### Scenario: Complete task from UI

- **WHEN** a user toggles the checkbox on a task row or uses complete in the detail panel
- **THEN** the task `status` updates via API
- **AND** completed tasks show struck-through or muted styling

#### Scenario: Open detail panel

- **WHEN** a user selects a task or opens `/app/tasks?taskId={id}`
- **THEN** the detail panel shows title, assignee, start/due time, reminder, description, and subtasks
- **AND** MUST NOT require navigating away from `/app/tasks`

### Requirement: Task due bot delivery

The task module SHALL call `ImBotApi.send` (via `im-api` only) with bot code `task-bot` to deliver a due reminder when a personal task assigned to a user is due within the configured remind window (`relayflow.task.due-remind-window`, default 24h) and remains `TODO`. Delivery MUST use target scope `SINGLE` for the task's `tenant_id` and `assignee_id`. The module MUST NOT call `NotifyInboxApi` or access `infra_notify` / `im_*` mappers directly. `ImBotApi.send` failures MUST NOT fail task create/update/list APIs (best-effort reach).

#### Scenario: Due task sends bot_dm on write path

- **WHEN** a `TODO` task is created or updated with `due_time` inside the remind window for an assignee in tenant T
- **THEN** `task-biz` calls `ImBotApi.send` with `botCode=task-bot` and target SINGLE `{ tenantId: T, userId: assignee }`
- **AND** uses `dedupeKey=TASK_DUE:{taskId}`
- **AND** message text identifies the task title and due time
- **AND** deep link metadata includes `route=/app/tasks?taskId={taskId}`, `entityType=task`, `entityId={taskId}`
- **AND** the reminder is persisted as `im_message` with `sender_type=bot`

#### Scenario: List path compensates missing due reminders

- **WHEN** a user requests `GET /app-api/task/item/page` and has a `TODO` task due within the window
- **THEN** the system attempts `ImBotApi.send` for that task via the same dedupe key (refresh or no-op if already delivered)

#### Scenario: Outside window or done skips send

- **WHEN** the task is `DONE`, has no `due_time`, or `due_time` is outside `[now, now+window]`
- **THEN** the system MUST NOT call `ImBotApi.send` solely for that task state

#### Scenario: Delivery failure does not block task API

- **WHEN** `ImBotApi.send` throws or returns a delivery failure
- **THEN** the task write or list API still succeeds
- **AND** the failure is logged server-side without exposing Bot enablement errors to the client

#### Scenario: No parallel notify inbox

- **WHEN** a due reminder is delivered
- **THEN** the system MUST NOT insert into `infra_notify`
- **AND** MUST NOT publish a business-required `domain=notify` envelope

### Requirement: Task title search API for workspace

The system SHALL expose `GET /app-api/task/item/search` for authenticated users to search their own tasks by title.

#### Scenario: Task search endpoint

- **WHEN** an authenticated user requests `GET /app-api/task/item/search?keyword=周报&limit=5`
- **THEN** the system returns up to 5 tasks where `assignee_id` is the current user and title matches the keyword
- **AND** results are scoped to the JWT tenant

### Requirement: Task due-range query for calendar projection

The system SHALL expose an authenticated workspace API to list the current user's TODO tasks whose `due_time` falls within a requested time window, for calendar projection. Results MUST be scoped to the JWT tenant and `assignee_id` equal to the current user. Tasks without `due_time`, with status `DONE`, or assigned to others MUST NOT appear. The API MUST NOT write `cal_*` tables.

#### Scenario: List due tasks in window

- **WHEN** an authenticated user requests `GET /app-api/task/item/due-range` with `from` and `to`
- **THEN** the system returns TODO tasks where `assignee_id` is the current user, `tenant_id` is the JWT tenant, and `due_time` is within the requested window
- **AND** each item includes at least `id`, `title`, `dueTime`, and `status`

#### Scenario: Empty when none due

- **WHEN** the user has no matching TODO tasks in the window
- **THEN** the system returns an empty list with `code=0`

#### Scenario: Bound size

- **WHEN** more than the configured maximum matching tasks exist in the window
- **THEN** the system returns at most that maximum (default 200)
- **AND** does not fail the request solely due to truncation

### Requirement: Optional TaskItemApi due-range for cross-domain read

The `task-api` module MAY expose a `TaskItemApi` method with the same due-range semantics for other domains. Calendar V1 projection MUST NOT require calendar-biz to call this method (frontend uses app-api). Any such API MUST NOT grant access to another user's tasks.

#### Scenario: Same-user scope on TaskItemApi

- **WHEN** a caller invokes `TaskItemApi` due-range with a tenant and user id
- **THEN** only TODO tasks assigned to that user in that tenant within the window are returned

### Requirement: Task detail fields

The system SHALL persist optional `start_time`, `description` (text), and `remind_before_minutes` on `task_item`, and SHALL expose get/update APIs for the detail panel. `due_time` remains the primary field for calendar projection and due Bot delivery.

#### Scenario: Update detail fields

- **WHEN** an authorized user updates start/due/remind/description on a task
- **THEN** the values persist
- **AND** a subsequent get returns the updated fields

#### Scenario: Invalid time range

- **WHEN** both start and due are set and start is after due
- **THEN** the system rejects with a business error

### Requirement: Subtasks one level deep

The system SHALL allow creating subtasks under a root task via `parent_id`. Subtasks MUST NOT themselves have children in this version. Parent detail MUST expose subtask list and completion progress (done count / total).

#### Scenario: Add subtask

- **WHEN** an authorized user adds a subtask with a non-empty title under a root task
- **THEN** a child `task_item` is created with `parent_id` set to the parent
- **AND** the parent's progress reflects the new child

#### Scenario: Reject nested subtask

- **WHEN** a user attempts to create a child under a task that already has a non-null `parent_id`
- **THEN** the system rejects the create

#### Scenario: Complete subtask

- **WHEN** a user marks a subtask done
- **THEN** parent progress updates
- **AND** completing all subtasks MUST NOT auto-complete the parent unless product UI explicitly does so (default: MUST NOT auto-complete parent)

### Requirement: Task followers and following inbox

The system SHALL allow users to follow tasks and list tasks they follow（「我关注的」）. Followers MAY read task detail and comment when collab APIs are enabled; followers MUST NOT edit core fields unless they are also assignee/creator (or future editor role).

#### Scenario: Follow task

- **WHEN** a user follows a task they can access
- **THEN** the task appears in their following list
- **AND** duplicate follow is idempotent

#### Scenario: Unfollow task

- **WHEN** a user unfollows a task
- **THEN** it no longer appears in「我关注的」

### Requirement: Task assignment

The system SHALL allow authorized users to replace or edit the task **assignee set** with one or more active members of the current tenant (validated via `system-api`). After the set changes, the task MUST appear in「我负责的」for each user in the set (if root) and MUST NOT appear for users removed from the set solely for that reason. The system MUST record **assigner** according to the assigner-field requirement when the assignment qualifies. Delivery via `task-bot` MUST be best-effort to newly added assignees and MUST NOT fail the assign API.

#### Scenario: Assign to members

- **WHEN** an authorized user sets assignees to members B and C in the same tenant
- **THEN** the assignee set becomes {B, C}
- **AND** the system attempts best-effort `ImBotApi.send` with `botCode=task-bot` to newly added assignees (SINGLE each) with deep link to the task

#### Scenario: Assign rejects non-member

- **WHEN** any target user in the assignee set is not an active member of the tenant
- **THEN** the system rejects the assign

### Requirement: Task comments

The system SHALL allow authorized users (assignee, creator, follower at minimum) to add plain-text comments on a task and list them chronologically.

#### Scenario: Add comment

- **WHEN** an authorized user posts a non-empty comment
- **THEN** the comment is persisted
- **AND** an activity entry of type commented is recorded

### Requirement: Task activity feed

The system SHALL record task activity events (create, field changes, subtask changes, follow, assign, comment) and expose:
1. per-task activity for the detail panel
2. a personal「动态」feed of activities on tasks the user created, is assigned to, or follows

#### Scenario: Personal dynamics

- **WHEN** a user opens the dynamics view
- **THEN** they see recent activities on tasks related to them, newest first
- **AND** MUST NOT see activities only related to unrelated tenants or unrelated tasks

#### Scenario: Detail activity

- **WHEN** a user views a task they can access
- **THEN** they can list that task's activity entries

### Requirement: Workspace task navigation entries

The `/app/tasks` left navigation SHALL enable「我负责的」, and after collab delivery SHALL enable「我关注的」and「动态」as functional entries (no longer permanent placeholders).

#### Scenario: Nav after collab

- **WHEN** collab APIs are integrated
- **THEN**「我关注的」and「动态」are usable
- **AND** each loads via store/API without page-local mock as source of truth

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

### Requirement: List custom single-select field UI (web)

Within a task list context, the workspace tasks UI MUST allow users with list edit rights to define list-scoped **single-select** custom fields (name + ordered options). Field schemas MUST NOT appear on quick-access contexts. Until custom-field API integrate, definitions and values MAY persist only in the client session (store mock).

#### Scenario: Create field and options

- **WHEN** a list OWNER/EDITOR creates a single-select field with two or more options
- **THEN** the field appears in that list's field list for the current session
- **AND** quick-access contexts MUST NOT show a list field schema editor

#### Scenario: Viewer read-only

- **WHEN** a list VIEWER opens the list
- **THEN** they MUST NOT be able to create, rename, reorder, or delete custom fields or options

### Requirement: Custom field as groupBy source (web)

When ViewConfig `groupBy.mode` is `FIELD` and `fieldKey` is `custom:{fieldId}` for a field belonging to the current list, the list and board MUST partition root tasks by that field's selected option. Empty/missing values MUST appear under「无分组」(`__empty__`). Dragging between partitions MUST update the mock value for the current session until API integrate.

#### Scenario: Board columns from custom select

- **WHEN** the user sets groupBy to a list custom single-select field
- **THEN** board columns (or list sections) match the field's options plus「无分组」for empty values

#### Scenario: Drag updates value

- **WHEN** the user drags a root task into an option column
- **THEN** that task's mock custom-field value becomes that option for the current list context

### Requirement: Detail panel custom field value (web)

In a list context, the task detail panel MUST show the current list's custom single-select fields and allow EDITOR+/OWNER to change or clear the value (clear →「无分组」). Values for other lists MUST NOT be edited unless the user switches list context.

#### Scenario: Edit value in detail

- **WHEN** an EDITOR sets a custom single-select value on a task in list L
- **THEN** that value is reflected in list L's groupBy partitions in the current session

### Requirement: List custom field persistence API

The system SHALL persist list-scoped single-select custom fields in `task_list_field`, options in `task_list_field_option`, and task values in `task_item_field_value` (EAV). REST under `/app-api/task/list-field` MUST support list/create/update/delete field, option CRUD, and PUT value. Deleting an option MUST clear referencing values; deleting a field MUST cascade options and values without deleting tasks. `PUT /app-api/task/item/group-move` MUST accept `fieldKey=custom:{fieldId}` with required `listId` and write the matching option by `value_key` (or clear for empty).

#### Scenario: Create and list fields

- **WHEN** an OWNER/EDITOR creates a SINGLE_SELECT field with options for a list
- **THEN** `GET /list-field/list?listId=` returns that field and options for list members

#### Scenario: Custom group-move

- **WHEN** an editable user calls group-move with `fieldKey=custom:{id}`, `listId`, and an option `value_key`
- **THEN** the task's EAV value for that field is updated to the option

### Requirement: Custom field API integration (web)

When viewing a task list, the workspace MUST load custom field definitions and values from `/app-api/task/list-field`. Create/update/delete field and options, put value, and custom `group-move` MUST call the API. The client MUST NOT keep an in-memory-only mock flag for production path.

#### Scenario: Persist after refresh

- **WHEN** a user creates a single-select field and sets a task value, then refreshes
- **THEN** the field and value remain

#### Scenario: Drag persists

- **WHEN** the user drags a task into an option column under custom groupBy
- **THEN** `PUT /item/group-move` succeeds and refresh keeps the bucket

