## ADDED Requirements

### Requirement: Personal custom groups on mine inbox (web)

When the user views「我负责的」with ViewConfig `groupBy.mode=PERSONAL_CUSTOM`, the list and board MUST partition tasks by the current user's personal groups. There MUST be exactly one default group. The user MUST be able to create additional groups and delete non-default groups (tasks return to the default group). Dragging a task between groups MUST update personal membership in the current session until mine-groups API integrate.

#### Scenario: Default group holds unassigned membership

- **WHEN** PERSONAL_CUSTOM is active and a mine task has no personal group membership yet
- **THEN** it appears under the default group

#### Scenario: Create and drag

- **WHEN** the user creates a group「本周」and drags a task into it
- **THEN** the task appears under「本周」in the current session
