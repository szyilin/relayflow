# task Specification

## Purpose

定义员工工作台个人任务（`task_item`）的领域模块、API 与 `/app/tasks` 页面对接行为。V1 仅「我负责的」CRUD；指派、看板、提醒见后续 change。
## Requirements
### Requirement: Task item domain module

The system SHALL provide a `task` domain with Maven modules `relayflow-module-task-api` and `relayflow-module-task-biz`, and persist task data in tables prefixed with `task_`.

#### Scenario: Task table naming

- **WHEN** a new task table is created
- **THEN** its name starts with `task_`
- **AND** it includes `tenant_id BIGINT NOT NULL`

### Requirement: Personal task CRUD for workspace

The system SHALL allow authenticated workspace members to manage task items assigned to themselves within the current tenant.

#### Scenario: List my tasks

- **WHEN** a user requests `GET /app-api/task/item/page`
- **THEN** the system returns tasks where `assignee_id` equals the current user and `tenant_id` equals the JWT tenant
- **AND** supports optional filter by `status` (`TODO` or `DONE`)

#### Scenario: Create task

- **WHEN** a user posts `POST /app-api/task/item/create` with a non-empty title
- **THEN** the system creates a `task_item` with `assignee_id` and `creator_id` set to the current user
- **AND** default `status` is `TODO`

#### Scenario: Toggle task done

- **WHEN** a user posts `PUT /app-api/task/item/toggle-done` for a task they own
- **THEN** the system sets `status` to `DONE` or `TODO` according to the request

#### Scenario: Forbidden on others tasks

- **WHEN** a user attempts to update or delete a task where `assignee_id` is not the current user
- **THEN** the system rejects with business error `TASK_FORBIDDEN`

### Requirement: Workspace tasks page integration

The `/app/tasks` page SHALL use the task API via store layer instead of inline mock data.

#### Scenario: Create task from UI

- **WHEN** a user creates a task from `/app/tasks`
- **THEN** the task appears in the list without a full page reload
- **AND** persists after refresh

#### Scenario: Complete task from UI

- **WHEN** a user toggles the checkbox on a task row
- **THEN** the task `status` updates via API
- **AND** completed tasks show struck-through or muted styling

### Requirement: Task due bot delivery

The task module SHALL call `ImBotApi.send` (via `im-api` only) with bot code `task-bot` (or equivalent seeded Bot) to deliver a due reminder when a personal task assigned to a user is due within the configured remind window and remains `TODO`. The module MUST NOT call `NotifyInboxApi` or access `infra_notify` / `im_*` mappers directly.

#### Scenario: Due task sends bot_dm

- **WHEN** a due-remind condition is met for an assignee in tenant T
- **THEN** `task-biz` calls `ImBotApi.send` with target SINGLE `{ tenantId: T, userId: assignee }`
- **AND** uses a stable `dedupeKey` such as `TASK_DUE:{taskId}`
- **AND** the reminder is persisted as `im_message` with `sender_type=bot`

#### Scenario: No parallel notify inbox

- **WHEN** a due reminder is delivered
- **THEN** the system MUST NOT insert into `infra_notify`
- **AND** MUST NOT publish a business-required `domain=notify` envelope

#### Scenario: Sidebar placeholders remain

- **WHEN** a user views `/app/tasks`
- **THEN** only "我负责的" is active
- **AND** "我关注的" and "动态" remain disabled or placeholder until a future change

### Requirement: Task title search API for workspace

The system SHALL expose `GET /app-api/task/item/search` for authenticated users to search their own tasks by title.

#### Scenario: Task search endpoint

- **WHEN** an authenticated user requests `GET /app-api/task/item/search?keyword=周报&limit=5`
- **THEN** the system returns up to 5 tasks where `assignee_id` is the current user and title matches the keyword
- **AND** results are scoped to the JWT tenant

