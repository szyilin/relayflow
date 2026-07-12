## MODIFIED Requirements

### Requirement: Admin user management page layout

The admin user list at `/admin/system/user` SHALL present a department tree alongside the member table so administrators can browse users by organization unit (Feishu-style members-and-departments).

#### Scenario: Department tree navigation

- **WHEN** an administrator opens `/admin/system/user`
- **THEN** a department tree is shown adjacent to the user table
- **AND** selecting a tree node refreshes the table for that department's direct primary members

#### Scenario: Create user with contextual department

- **WHEN** an administrator creates a user from the user list while a department node is selected
- **THEN** the create form defaults the primary department to the selected node
