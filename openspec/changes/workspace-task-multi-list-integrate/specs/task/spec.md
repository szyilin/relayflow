## MODIFIED Requirements

### Requirement: Multi-list membership editor (web)

Task detail MUST persist list memberships via `PUT /app-api/task/item/list-memberships`. The client MUST NOT use an in-memory-only mock flag. Removing a list MUST NOT delete the task.

#### Scenario: Persist across refresh

- **WHEN** the user adds a task to two lists and refreshes
- **THEN** both memberships remain after reload
