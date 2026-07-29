## ADDED Requirements

### Requirement: Workspace agent uses live APIs (skeleton)

After integrate, the workspace Agent shell SHALL load Agents and session chat through live `/app-api/agent` APIs without temporary store mock data for those flows.

#### Scenario: Chat round-trip against server

- **WHEN** a member sends a message in an Agent session with server running
- **THEN** the UI displays the assistant reply from the live runtime
- **AND** temporary mock data for that flow is removed
