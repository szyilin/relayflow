## ADDED Requirements

### Requirement: Persist personal mine groups

The system MUST store per-user personal groups in `task_mine_group` and task membership in `task_mine_group_item`. Each user MUST have exactly one default group. Deleting a non-default group MUST reassign its memberships to the default group and MUST NOT delete tasks. APIs under `/app-api/task/mine-group` MUST only read/write the current user's groups.

#### Scenario: Ensure default on list

- **WHEN** the user calls `GET /app-api/task/mine-group/list` and has no groups yet
- **THEN** the system creates a default group and returns it

#### Scenario: Delete non-default returns tasks to default

- **WHEN** the user deletes a non-default group that has memberships
- **THEN** those memberships point to the default group and the group row is soft-deleted (or removed from active set)
