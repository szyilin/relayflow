## ADDED Requirements

### Requirement: Workspace agent shell UI (skeleton)

The workspace SHALL provide an Agent product shell where members can browse Agents, start a new task session, view session history, and chat. Agent chat content MUST NOT be presented as ordinary IM conversation list items.

#### Scenario: New task opens agent chat surface

- **WHEN** a member starts a new task and selects an Agent
- **THEN** the UI opens an Agent chat session surface
- **AND** that surface is not the `/app/messages` conversation list
