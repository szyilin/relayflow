## MODIFIED Requirements

### Requirement: Multi-assignee editor on task detail

The workspace task detail panel MUST allow editing a task's assignee set as zero or more tenant members. Changes MUST persist via `PUT /app-api/task/item/assignees`. After removing the current user from the set, the task MUST NOT appear in「我负责的」on refresh / list reload.

#### Scenario: Persist multiple assignees

- **WHEN** a user saves assignees A and B then refreshes the task detail
- **THEN** both A and B remain shown as assignees

#### Scenario: Remove self drops from mine

- **WHEN** the current user removes themselves and saves while on「我负责的」
- **THEN** after list refresh the task is absent from「我负责的」
