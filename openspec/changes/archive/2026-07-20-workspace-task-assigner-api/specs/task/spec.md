## ADDED Requirements

### Requirement: Assigner persistence on assignee-set replace

When an authorized user replaces a task's assignee set, the system MUST set `assignerId` to the current user if the resulting set does not contain the current user and contains at least one other user; otherwise `assignerId` MUST be null. `GET /page?scope=ASSIGNED_BY_ME` MUST return root tasks where `assignerId` is the current user and the current user is not in the assignee set.

#### Scenario: Assign to others records assigner

- **WHEN** user A replaces assignees with only user B
- **THEN** `assignerId` becomes A and B appears in「我负责的」while A sees the task under「我分配的」
