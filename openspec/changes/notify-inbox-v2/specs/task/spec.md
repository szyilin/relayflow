## ADDED Requirements

### Requirement: Task due notification producer

The task module SHALL call `NotifyInboxApi` (via `infra-api` only) to push a `TASK_DUE` notification when a personal task assigned to a user is due within the configured remind window and remains `TODO`. The module MUST NOT access `infra_notify` mappers directly.

#### Scenario: Create task due soon

- **WHEN** a user creates a `TODO` task with `due_time` within the configured due-remind window (default 24 hours from now)
- **THEN** the system pushes a notification with `type=TASK_DUE` to that user as assignee
- **AND** the notification `dedupeKey` uniquely identifies the task (e.g. `task:{taskId}`)
- **AND** payload includes `route` under `/app/tasks` and the task `entityId`

#### Scenario: Update due time into window

- **WHEN** a user updates a `TODO` task so that `due_time` enters the due-remind window
- **THEN** the system pushes or refreshes the same `TASK_DUE` notification via dedupe

#### Scenario: Task outside window does not notify

- **WHEN** a user creates a `TODO` task with null `due_time` or `due_time` beyond the remind window
- **THEN** the system does not push a `TASK_DUE` notification solely due to that create

#### Scenario: Cross-module dependency rule

- **WHEN** task-biz produces a due notification
- **THEN** it depends only on `NotifyInboxApi` from `infra-api`
- **AND** does not depend on `infra-biz` implementation types

### Requirement: Lazy due-notify compensation on list

When listing the current user's tasks, the task module MUST perform a lightweight compensation check to push missing `TASK_DUE` notifications for owned `TODO` tasks already inside the remind window. Full multi-tenant cron scanning is NOT required in this change.

#### Scenario: List triggers missing due notify

- **WHEN** a user requests `GET /app-api/task/item/page` and has a `TODO` task due within the window without an unread `TASK_DUE` for that dedupe key
- **THEN** the system pushes that notification so the inbox catches up without a global cron
