## ADDED Requirements

### Requirement: Persist multi-assignee set

The system MUST store task assignees in `task_item_assignee` and expose `PUT /app-api/task/item/assignees` to replace the full set. Responses MUST include `assigneeIds` and a projected `assigneeId`. Default「我负责的」listing MUST return root tasks where the current user is in the assignee set. Existing `PUT /assign` MUST remain as a single-assignee compatibility write.

#### Scenario: Replace assignees

- **WHEN** an authorized user PUTs assigneeIds [B, C]
- **THEN** the assignee set becomes {B, C}
- **AND** projected assigneeId is the stable first element of the set

#### Scenario: Mine contains me

- **WHEN** user A is in a task's assignee set and requests the default mine page
- **THEN** that root task is returned
