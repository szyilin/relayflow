## ADDED Requirements

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
