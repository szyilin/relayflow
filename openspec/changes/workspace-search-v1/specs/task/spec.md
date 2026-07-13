## ADDED Requirements

### Requirement: Task title search API for workspace

The system SHALL expose `GET /app-api/task/item/search` for authenticated users to search their own tasks by title.

#### Scenario: Task search endpoint

- **WHEN** an authenticated user requests `GET /app-api/task/item/search?keyword=周报&limit=5`
- **THEN** the system returns up to 5 tasks where `assignee_id` is the current user and title matches the keyword
- **AND** results are scoped to the JWT tenant
