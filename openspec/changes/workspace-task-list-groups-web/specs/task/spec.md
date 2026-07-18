## ADDED Requirements

### Requirement: List-local groups UI (web)

When the user views a task list and ViewConfig `groupBy` is not a field group (`null` or `LIST_GROUP`), the list and board MUST partition tasks by that list's groups. There MUST be one default group per list. Users with mutate permission MUST be able to create groups, delete non-default groups (tasks return to default), and drag tasks between groups in the current session until list-groups API integrate.

#### Scenario: Default group

- **WHEN** LIST_GROUP presentation is active and a list task has no group membership yet
- **THEN** it appears under the default group

#### Scenario: Create and drag

- **WHEN** the user creates a group and drags a task into it
- **THEN** the task appears under that group in the current session
