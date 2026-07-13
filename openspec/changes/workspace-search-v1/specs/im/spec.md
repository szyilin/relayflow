## ADDED Requirements

### Requirement: Conversation search API for workspace

The system SHALL expose `GET /app-api/im/conversation/search` for authenticated users to search their conversations by title or direct-chat peer display name.

#### Scenario: Conversation search endpoint

- **WHEN** an authenticated user requests `GET /app-api/im/conversation/search?keyword=项目&limit=5`
- **THEN** the system returns up to 5 conversations where the user is a member and the keyword matches
- **AND** results are scoped to the JWT tenant
