## ADDED Requirements

### Requirement: Group-move persists field bucket changes

The system MUST expose `PUT /app-api/task/item/group-move` that updates a root task's grouped system field (`status`, `dueTime`, or `assigneeId`) according to the request body. Empty bucket MUST be represented by `value` null or `__empty__` for `dueTime` and `assigneeId`. Clearing `status` MUST be rejected with `TASK_GROUP_MOVE_INVALID`. Callers without edit permission MUST receive `TASK_FORBIDDEN`. Subtasks MUST be rejected. Existing `PUT /board-move` MAY remain as a status-only compatibility endpoint.

#### Scenario: Move to another status bucket

- **WHEN** an editable root task is group-moved with `fieldKey=status` and a valid status value
- **THEN** the task status is persisted
- **AND** boardRank is set (append or relative to beforeId)

#### Scenario: Clear due date into 无分组

- **WHEN** group-move uses `fieldKey=dueTime` and `value` is null or `__empty__`
- **THEN** the task `dueTime` is set to null
