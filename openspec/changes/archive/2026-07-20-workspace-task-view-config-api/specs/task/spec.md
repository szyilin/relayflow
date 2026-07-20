## ADDED Requirements

### Requirement: Persist task view config

The system SHALL persist ViewConfig per context via `GET /app-api/task/view-config/get` and `PUT /app-api/task/view-config/save`. Personal contexts MUST be private to the current user. LIST context MUST use a shared default readable by list members; save MUST require OWNER or EDITOR. Missing rows MUST return a documented default config. VIEWER MUST NOT save LIST shared config (`TASK_VIEW_CONFIG_FORBIDDEN`).

#### Scenario: Get default when missing

- **WHEN** a user requests view config for `MINE` and no row exists
- **THEN** the system returns the default ViewConfig without error

#### Scenario: Viewer cannot save list config

- **WHEN** a VIEWER saves LIST view config
- **THEN** the system rejects with `TASK_VIEW_CONFIG_FORBIDDEN`
