## ADDED Requirements

### Requirement: Bindable skill catalog

The system SHALL maintain a catalog of built-in skills that can be bound to an Agent via `skillIds` (persisted as `agent_def.skill_ids_json`). At runtime, bound skills SHALL be exposed as Spring AI tools to the agent loop. Built-in skill implementations MUST call other domains only via `*-api` under the acting user's authorization (tenant + user from the current security context).

The V1 catalog SHALL include at least:

- `task.search` — search the current user's tasks by keyword
- `task.list_due` — list the current user's tasks with due time in a range

Unknown skill codes MUST be rejected when saving an Agent definition. Unbound skills MUST NOT be available as tools for that Agent.

#### Scenario: Bound skill available as tool

- **WHEN** an Agent has a built-in skill bound
- **AND** a session turn runs
- **THEN** the runtime may invoke that skill as a tool
- **AND** the implementation does not depend on foreign `*-biz` modules

#### Scenario: Unbound skill not available

- **WHEN** an Agent does not bind `task.search`
- **AND** a session turn runs
- **THEN** the `task_search` tool is not provided to the model

#### Scenario: Reject unknown skill on save

- **WHEN** a client saves an Agent with a skill code not in the catalog
- **THEN** the system rejects the save
