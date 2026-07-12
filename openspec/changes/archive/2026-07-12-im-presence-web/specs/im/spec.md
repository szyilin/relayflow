## ADDED Requirements

### Requirement: Workspace presence UI

The workspace SHALL display online or offline indicators for relevant org members in the messages and contacts asides.

#### Scenario: Direct chat peer presence

- **WHEN** the user opens a direct conversation
- **THEN** the aside shows the peer member's online status

#### Scenario: Contacts member presence

- **WHEN** the contacts page lists members in the aside or detail panel
- **THEN** each member row includes an online or offline indicator

#### Scenario: Presence mock fallback

- **WHEN** presence batch API is unavailable
- **THEN** the store uses mock online flags without page-level mock imports
