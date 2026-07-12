## ADDED Requirements

### Requirement: Workspace group chat UI

The workspace `/app/messages` page SHALL support creating group conversations, inviting members, displaying group threads with member sidebar, and rendering system messages (e.g. member joined).

#### Scenario: Create group from messages page

- **WHEN** the user opens the create-group dialog, enters a group name, selects at least one org member, and confirms
- **THEN** a new `type=group` conversation appears in the conversation list
- **AND** the user is navigated to that group thread

#### Scenario: System message for member join

- **WHEN** a member is added to a group (create or invite)
- **THEN** the message list includes a `senderType=system` message describing the join event
- **AND** the UI renders it as a centered system line, not a user bubble

#### Scenario: Group API mock fallback

- **WHEN** group REST endpoints are not yet implemented
- **THEN** the store uses mock data for group create, invite, and member list
- **AND** direct chat continues to use real APIs without importing mocks in pages

#### Scenario: Group member sidebar

- **WHEN** the user selects an active `type=group` conversation
- **THEN** the aside panel lists current group members
- **AND** provides an action to invite additional org members
