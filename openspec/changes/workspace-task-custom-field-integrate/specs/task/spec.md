## ADDED Requirements

### Requirement: Custom field API integration (web)

When viewing a task list, the workspace MUST load custom field definitions and values from `/app-api/task/list-field`. Create/update/delete field and options, put value, and custom `group-move` MUST call the API. The client MUST NOT keep an in-memory-only mock flag for production path.

#### Scenario: Persist after refresh

- **WHEN** a user creates a single-select field and sets a task value, then refreshes
- **THEN** the field and value remain

#### Scenario: Drag persists

- **WHEN** the user drags a task into an option column under custom groupBy
- **THEN** `PUT /item/group-move` succeeds and refresh keeps the bucket
