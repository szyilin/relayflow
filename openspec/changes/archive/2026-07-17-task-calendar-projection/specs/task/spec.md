## ADDED Requirements

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
