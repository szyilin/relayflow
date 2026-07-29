## ADDED Requirements

### Requirement: Workspace section in user preference

The user preference document MUST include a `settings.workspace` object for per-tenant-member workspace shell UI preferences. Keys MUST be merged with code defaults on GET (C-class). Defaults MUST include `railOrder` (string array) equal to the product default workspace rail order. PUT MUST accept a sanitized `railOrder` array of known rail ids; unknown ids MUST be dropped; an empty or absent array MUST merge to the code default.

#### Scenario: Default workspace settings

- **WHEN** a member has no preference row or no `workspace` keys
- **THEN** GET preference returns code defaults including `workspace.railOrder` with the product default id sequence

#### Scenario: Save rail order

- **WHEN** a member updates `settings.workspace.railOrder` via PUT preference
- **THEN** the values persist for `(tenant_id, user_id)`
- **AND** other settings sections (e.g. `general`, `im`, `calendar`, `task`) remain intact under deep merge rules

#### Scenario: Sanitize unknown rail ids

- **WHEN** PUT includes `railOrder` containing unknown or duplicate ids
- **THEN** unknown ids are dropped, duplicates collapsed, and missing known ids are filled from code defaults on the effective GET document
