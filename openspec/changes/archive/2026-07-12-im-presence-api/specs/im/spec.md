## ADDED Requirements

### Requirement: Batch presence query

The system SHALL allow authenticated workspace members to query online status for a batch of users in the current tenant.

#### Scenario: Batch query same tenant users

- **WHEN** a member requests presence for a list of user IDs in the current tenant
- **THEN** the API returns each user's online boolean based on active WebSocket sessions

#### Scenario: Ignore foreign tenant ids

- **WHEN** the request includes user IDs outside the current tenant
- **THEN** those IDs are omitted from the response without error
