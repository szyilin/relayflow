## ADDED Requirements

### Requirement: Member search API for workspace

The system SHALL expose `GET /app-api/system/member/search` for authenticated workspace members to search colleagues in the current tenant by nickname or mobile.

#### Scenario: Member search endpoint

- **WHEN** an authenticated user requests `GET /app-api/system/member/search?keyword=李&limit=5`
- **THEN** the system returns up to 5 matching active tenant members
- **AND** does not require `sys_permission` codes beyond valid membership
