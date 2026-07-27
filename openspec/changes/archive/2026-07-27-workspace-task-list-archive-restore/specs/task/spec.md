## ADDED Requirements

### Requirement: List archive discoverability and restore

The system SHALL treat list archive as a soft hide: archiving MUST set `archived=true` and MUST NOT delete the list row or its tasks. The workspace MUST expose APIs and UI for members to list archived lists they belong to, and for OWNER to restore (`archived=false`). `GET /app-api/task/list/mine` MUST continue to return only non-archived lists.

#### Scenario: List archived

- **WHEN** a member calls `GET /app-api/task/list/archived`
- **THEN** the system returns lists where the user is a member and `archived` is true
- **AND** each item includes `myRole`

#### Scenario: Restore archived list

- **WHEN** an OWNER calls `POST /app-api/task/list/unarchive` with a list id
- **THEN** the list's `archived` becomes false
- **AND** the list appears again in `GET /list/mine`

#### Scenario: Non-owner cannot restore

- **WHEN** a non-OWNER member attempts unarchive
- **THEN** the system rejects with `TASK_LIST_FORBIDDEN` (or equivalent role error)

#### Scenario: Archive confirmation UX

- **WHEN** an OWNER archives a list from `/app/tasks`
- **THEN** the UI confirms that archive is reversible via「已归档」and does not claim permanent deletion
- **AND** after archive the list leaves the active lists nav and can be restored from the archived section
