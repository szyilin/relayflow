## MODIFIED Requirements

### Requirement: Field group-by presentation on workspace tasks

When the active ViewConfig `groupBy` mode is `FIELD`, the `/app/tasks` list and board MUST partition root tasks by that field's value. Empty/missing values MUST appear under「无分组». Dragging a task to another partition MUST persist via `PUT /app-api/task/item/group-move` (optimistic UI with rollback on failure). When `groupBy` is null, the list MUST render flat and the board MAY show a single「全部」column.

#### Scenario: Board drag persists after refresh

- **WHEN** the user drags a card between field buckets and refreshes
- **THEN** the task remains in the target bucket according to the persisted field value
