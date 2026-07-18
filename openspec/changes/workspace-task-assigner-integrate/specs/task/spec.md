## MODIFIED Requirements

### Requirement: Show assigner on task detail

When a task has a non-null `assignerId` from the API, the workspace task detail panel MUST display it. After user A assigns a task solely to user B, A MUST see it under「我分配的」and B under「我负责的」after list refresh.

#### Scenario: Cross-user assigner flow

- **WHEN** user A saves assignees to only B then opens「我分配的」
- **THEN** the task appears for A
- **AND** B sees it under「我负责的」
