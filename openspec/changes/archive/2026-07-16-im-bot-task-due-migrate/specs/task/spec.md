## MODIFIED Requirements

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

#### Scenario: Sidebar placeholders remain

- **WHEN** a user views `/app/tasks`
- **THEN** only "我负责的" is active
- **AND** "我关注的" and "动态" remain disabled or placeholder until a future change
