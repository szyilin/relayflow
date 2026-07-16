## MODIFIED Requirements

### Requirement: Member invite notification trigger

The system SHALL deliver a user-visible invite reminder via `ImBotApi.send` (bot code `invite-helper` or equivalent seeded Bot) when an administrator successfully invites a member by mobile. The system MUST NOT write `infra_notify` or call `NotifyInboxApi`.

#### Scenario: Invite pushes bot_dm reminder

- **WHEN** `POST /admin-api/system/user/invite` succeeds and creates or updates a `NOT_JOINED` membership
- **THEN** the system calls `ImBotApi.send` with invite content including tenant name and inviter display name
- **AND** uses an explicit delivery target (default SINGLE current inviting tenant unless a later invite-slice chooses fanout)
- **AND** persists the reminder as `im_message` with `sender_type=bot` in the appropriate bot_dm

#### Scenario: Duplicate invite refreshes via dedupe

- **WHEN** the same mobile/user already has an unread invite reminder for the same tenant under the same dedupe key
- **THEN** the system refreshes or ignores per `ImBotApi` dedupe rules instead of creating duplicate unread bot messages
