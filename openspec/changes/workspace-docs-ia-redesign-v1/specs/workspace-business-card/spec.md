## ADDED Requirements

### Requirement: Business card opens from owner chip

The personal business card MUST be openable from the workspace owner/user chip in list contexts (documents, drive, tasks), using the same self/peer card component as contacts.

#### Scenario: Peer owner from docs list

- **WHEN** a member clicks an owner chip for another user on a docs Home or library row
- **THEN** `WorkspaceBusinessCard` opens in peer mode for that `userId`

#### Scenario: Self owner from list

- **WHEN** a member clicks an owner chip for themselves
- **THEN** `WorkspaceBusinessCard` opens in self mode
