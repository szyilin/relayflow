## MODIFIED Requirements

### Requirement: Workspace owner chip component

The workspace SHALL provide a reusable owner/user chip component that displays the member's avatar and display name, and opens the existing personal business card on click. The chip MUST be usable from document lists, drive listings, task assignee surfaces, **and admin console surfaces that display document owners or storage members** (e.g. admin docs file ops and storage usage). When no avatar URL is available, the letter fallback MUST use the workspace deterministic gradient letter-avatar treatment (stable hash from `userId` or name), NOT a neutral gray-only circle.

#### Scenario: Chip shows avatar and name

- **WHEN** a list row renders an owner or assignee with a known `userId`
- **THEN** the UI shows an avatar (image or letter fallback) and a human display name
- **AND** MUST NOT use the bare word「我」as the only visible name for the current user (a secondary「我」badge beside the real name is allowed)

#### Scenario: Click opens business card

- **WHEN** the member clicks the owner chip
- **THEN** a peer or self `WorkspaceBusinessCard` opens (anchored popover or equivalent)
- **AND** the click MUST NOT alone open the underlying document or task row

#### Scenario: Missing avatar still clickable

- **WHEN** no avatar URL is available for the user
- **THEN** the chip still renders a letter avatar on a deterministic gradient background and remains clickable to open the business card
- **AND** the gradient color group MUST be stable for the same `userId` across pages

#### Scenario: Admin docs reuse same chip

- **WHEN** an admin docs file or storage usage row renders an owner/member
- **THEN** the same `WorkspaceOwnerChip` component is used
- **AND** MUST NOT introduce a parallel admin-only plain-text id-under-name identity cell as the primary pattern
- **AND** letter fallbacks MUST use the same gradient treatment as the workspace
