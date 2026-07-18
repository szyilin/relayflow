## ADDED Requirements

### Requirement: Show assigner on task detail

When a task has a non-null `assignerId`, the workspace task detail panel MUST display the assigner (分配人) as a read-only field distinct from the assignee set. When `assignerId` is null, the panel MAY omit the field or show「无」. Changing the assignee set remains the write path that updates assigner per product rules; this requirement does not add a separate assigner editor.

#### Scenario: Assigned-by-me task shows assigner

- **WHEN** the current user opens a task from「我分配的」that they assigned to others
- **THEN** the detail shows the current user (or resolved nickname) as 分配人
- **AND** the assignee chips show the other members
