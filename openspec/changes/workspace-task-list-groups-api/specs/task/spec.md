## ADDED Requirements

### Requirement: Persist list-local groups

The system MUST store per-list groups in `task_list_group` and task placement via `task_list_item.group_id`. Each list MUST have exactly one default group. Deleting a non-default group MUST reassign memberships to the default group without deleting tasks. APIs under `/app-api/task/list-group` MUST authorize list members appropriately.

#### Scenario: Ensure default on list

- **WHEN** `GET /list-group/list?listId=` is called and the list has no default group
- **THEN** the system creates a default group and returns it
