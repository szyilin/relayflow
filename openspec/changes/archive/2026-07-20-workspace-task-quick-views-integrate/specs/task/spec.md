## ADDED Requirements

### Requirement: Quick views use real page API without local mocks

The `/app/tasks` quick-access contexts「全部任务」and「我分配的」MUST load via `GET /app-api/task/item/page` with `scope=ALL` and `scope=ASSIGNED_BY_ME` respectively. The workspace tasks store MUST NOT keep temporary client-side merge or demo rows for these scopes after integrate.

#### Scenario: All tasks from API

- **WHEN** the user opens「全部任务」with a running backend
- **THEN** the list comes from `scope=ALL` page API without local merge mocks

#### Scenario: Assigned-by-me from API

- **WHEN** the user opens「我分配的」
- **THEN** the list comes from `scope=ASSIGNED_BY_ME` without demo placeholder tasks
