## MODIFIED Requirements

### Requirement: System bot catalog for organization reach

The platform system bot catalog MUST treat `org-assistant`（组织助手）as the sole bot code for organization-admin business reach including member invitations. Bot code `invite-helper` MUST NOT remain an active delivery entry.

#### Scenario: org-assistant covers invites

- **GIVEN** seeded system bots
- **WHEN** a caller sends an organization invite reminder
- **THEN** the caller uses `botCode=org-assistant`
- **AND** MUST NOT use `invite-helper`

#### Scenario: invite-helper retired

- **GIVEN** a database that previously seeded `invite-helper`
- **WHEN** migrations for this change have applied
- **THEN** `invite-helper` is soft-deleted or disabled and MUST NOT be selectable as an active bot for `ImBotApi.send`
