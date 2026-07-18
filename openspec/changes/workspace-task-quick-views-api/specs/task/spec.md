## ADDED Requirements

### Requirement: Quick-view page scopes ALL and ASSIGNED_BY_ME

The system SHALL support `GET /app-api/task/item/page` with `scope=ALL` returning root tasks visible to the current user as the union of: assignee is self, creator is self, user follows the task, or task belongs to a non-archived list the user is a member of. The system SHALL support `scope=ASSIGNED_BY_ME` returning root tasks where `assigner_id` equals the current user and `assignee_id` is not the current user. Optional `status` filter MUST apply to both scopes. The system MUST NOT return arbitrary tenant-wide tasks under `ALL`.

#### Scenario: Page ALL excludes unrelated tenant tasks

- **WHEN** a user requests page with `scope=ALL`
- **THEN** only tasks matching the visibility union for that user in the JWT tenant are returned

#### Scenario: Page assigned-by-me

- **WHEN** user A assigned a task to B and requests `scope=ASSIGNED_BY_ME`
- **THEN** that root task is included
- **AND** tasks where A is still the assignee MUST NOT be included under this scope

### Requirement: Task assigner persistence

The system SHALL persist `assigner_id` on `task_item`. When an authorized user assigns a task to a different active member, `assigner_id` MUST become the assigning user. When a user assigns a task to themselves, `assigner_id` MUST be cleared. Task detail/list responses MUST include `assignerId` (nullable).

#### Scenario: Assign to other sets assigner

- **WHEN** user A assigns a task to user B
- **THEN** `assignee_id` becomes B and `assigner_id` becomes A
