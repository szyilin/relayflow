## ADDED Requirements

### Requirement: Start direct chat from contacts

The workspace contacts flow SHALL allow a member to open a direct chat with a colleague from the contact profile card.

#### Scenario: Message action from profile card

- **WHEN** a user clicks「消息」on a contact profile in `/app/contacts`
- **THEN** the client navigates to `/app/messages` targeting that user's `peerUserId`
- **AND** the first sent message MAY lazy-create the direct conversation per im-direct-chat contract
