## ADDED Requirements

### Requirement: Persist multi-list membership

The system MUST store root-task list memberships in `task_list_item`. Listing by `listId` MUST return tasks that have a membership row for that list. `PUT /app-api/task/item/list-memberships` MUST fully replace memberships without deleting the task. Task responses MUST include `listIds` and a compat `listId` projection.

#### Scenario: Page by list uses membership table

- **WHEN** a client requests `GET /item/page?listId=X`
- **THEN** only tasks with a non-deleted `task_list_item` for list X are returned

#### Scenario: Replace memberships keeps task

- **WHEN** a user replaces memberships to remove the last list
- **THEN** the task row remains and `listIds` is empty
