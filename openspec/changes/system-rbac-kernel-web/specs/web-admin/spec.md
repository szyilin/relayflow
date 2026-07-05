## ADDED Requirements

### Requirement: Frontend permission gating

The admin frontend SHALL load permission codes after login and hide navigation entries when the user lacks the associated permission code.

#### Scenario: Sidebar hides unauthorized menu

- **WHEN** the logged-in user's permission set does not include `system:dept:list`
- **THEN** the admin sidebar MUST NOT show the department management entry

#### Scenario: Super admin sees gated entries

- **WHEN** the logged-in user has `system:user:list` and `system:dept:list`
- **THEN** the user management and department management nav entries are visible

#### Scenario: Refresh reloads permissions

- **WHEN** the user reloads the browser with a valid stored token on an admin route
- **THEN** the frontend fetches permission info again before rendering filtered navigation
