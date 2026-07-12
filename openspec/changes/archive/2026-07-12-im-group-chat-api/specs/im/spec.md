## ADDED Requirements

### Requirement: Group chat REST API

The system SHALL expose app-api endpoints to create groups, invite members, and list group members; group messages SHALL reuse the existing message send/list endpoints with `conversationId`.

#### Scenario: Create group

- **WHEN** an authenticated member POSTs `/app-api/im/group/create` with a valid name and at least one member user id
- **THEN** the system creates `im_conversation(type=group)`, `im_group`, and membership rows (creator as owner)
- **AND** returns `conversationId` and `groupId`

#### Scenario: Invite members

- **WHEN** a group member POSTs `/app-api/im/group/members/add` with new user ids
- **THEN** new members are added to `im_conversation_member`
- **AND** a system message is persisted for each join event

#### Scenario: List group in conversation list

- **WHEN** the user requests `/app-api/im/conversation/list`
- **THEN** direct and group conversations are returned
- **AND** group items include `memberCount`

#### Scenario: Group message sender nickname

- **WHEN** the user lists messages in a group conversation
- **THEN** user messages include `senderNickname` for display
