## ADDED Requirements

### Requirement: Multi-list membership editor (web)

Task detail MUST show the task's list memberships and MUST allow the user to add or remove lists without deleting the task. Until multi-list API integrate, changes MAY persist only in the client session via a local mock flag.

#### Scenario: Belong to two lists locally

- **WHEN** the user opens a task and selects two lists in the membership editor
- **THEN** both list badges appear on the detail and the task remains available (not deleted)

#### Scenario: Remove from current list view

- **WHEN** the user is viewing list A and removes list A from the task's memberships
- **THEN** the task disappears from that list view but is not deleted
