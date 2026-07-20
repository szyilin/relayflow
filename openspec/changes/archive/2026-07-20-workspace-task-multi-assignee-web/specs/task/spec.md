## ADDED Requirements

### Requirement: Multi-assignee editor on task detail

The workspace task detail panel MUST allow editing a task's assignee set as zero or more tenant members. The UI MUST show all current assignees. Until multi-assignee API integrate, changes MAY persist only in client session state.

#### Scenario: Select multiple assignees

- **WHEN** a user opens the assignee editor and selects members A and B then saves
- **THEN** the detail shows both A and B as assignees in the current session

#### Scenario: Remove self from mine (client preview)

- **WHEN** the current user removes themselves from the assignee set while viewing「我负责的」
- **THEN** the task disappears from the current mine list in the client session
