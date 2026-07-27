## ADDED Requirements

### Requirement: Workspace owner chip component web

The workspace web app MUST provide `WorkspaceOwnerChip` showing avatar and display name, opening `WorkspaceBusinessCard` on click without triggering the parent row open action.

#### Scenario: Chip opens card

- **WHEN** the member clicks an owner chip
- **THEN** the business card opens for that userId
- **AND** the parent list row MUST NOT open solely from that click

#### Scenario: Self name not bare 我

- **WHEN** the chip represents the current user
- **THEN** a real display name is shown
- **AND** an optional「我」badge MAY appear beside the name
