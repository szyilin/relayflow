# task Specification

## Purpose

定义员工工作台协作任务（`task_item`）的领域模块、API 与 `/app/tasks` 页面对接行为。含详情字段、子任务、关注/评论/动态/指派；清单容器、看板、自定义字段见后续 change。

## Requirements

### Requirement: Task item domain module

The system SHALL provide a `task` domain with Maven modules `relayflow-module-task-api` and `relayflow-module-task-biz`, and persist task data in tables prefixed with `task_`.

#### Scenario: Task table naming

- **WHEN** a new task table is created
- **THEN** its name starts with `task_`
- **AND** it includes `tenant_id BIGINT NOT NULL`

### Requirement: Personal task CRUD for workspace

The system SHALL allow authenticated workspace members to manage task items assigned to themselves within the current tenant. Task items MUST support optional `start_time`, `due_time`, `description`, and `remind_before_minutes` in addition to title and status. List endpoints for「我负责的」MUST return only **root** tasks (`parent_id` is null) unless a request explicitly asks for children.

#### Scenario: List my tasks

- **WHEN** a user requests `GET /app-api/task/item/page`
- **THEN** the system returns root tasks where `assignee_id` equals the current user and `tenant_id` equals the JWT tenant
- **AND** supports optional filter by `status` (`TODO` or `DONE`)

#### Scenario: Create task

- **WHEN** a user posts `POST /app-api/task/item/create` with a non-empty title
- **THEN** the system creates a `task_item` with `assignee_id` and `creator_id` set to the current user (unless assignee is explicitly set and authorized in a later collab requirement)
- **AND** default `status` is `TODO`
- **AND** `parent_id` is null for top-level creates

#### Scenario: Toggle task done

- **WHEN** a user posts `PUT /app-api/task/item/toggle-done` for a task they may complete
- **THEN** the system sets `status` to `DONE` or `TODO` according to the request

#### Scenario: Forbidden on others tasks

- **WHEN** a user attempts to update or delete a task where they are neither assignee nor otherwise authorized (e.g. follower read-only)
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

The system SHALL allow changing `assignee_id` to another active member of the current tenant (validated via `system-api`). After assignment, the task MUST appear in the new assignee's「我负责的」root list (if root). Delivery via `task-bot` MUST be best-effort and MUST NOT fail the assign API.

#### Scenario: Assign to member

- **WHEN** an authorized user assigns a task to member B in the same tenant
- **THEN** `assignee_id` becomes B
- **AND** the system attempts `ImBotApi.send` with `botCode=task-bot` to B (SINGLE) with deep link to the task

#### Scenario: Assign rejects non-member

- **WHEN** the target user is not an active member of the tenant
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

