## MODIFIED Requirements

### Requirement: Member invite notification trigger

The system SHALL deliver a user-visible invite reminder via `ImBotApi.send` (bot code `org-assistant`) when an administrator successfully invites a member by mobile. The system MUST NOT write `infra_notify` or call `NotifyInboxApi`. Invite reminders MUST NOT use a separate `invite-helper` bot.

#### Scenario: Invite pushes bot_dm reminder

- **WHEN** `POST /admin-api/system/user/invite` succeeds and creates or updates a `NOT_JOINED` membership
- **AND** the invitee has at least one `ACTIVE` tenant membership
- **THEN** the system calls `ImBotApi.send` with invite content including tenant name and inviter display name
- **AND** uses `botCode=org-assistant` with target scope `ALL_ACTIVE_MEMBERSHIPS`
- **AND** persists the reminder as `im_message` with `sender_type=bot` in the appropriate bot_dm

#### Scenario: Invite with no active membership skips bot delivery

- **WHEN** the invitee has no `ACTIVE` memberships
- **THEN** the invite still succeeds
- **AND** bot delivery MAY be skipped (registration pending banner remains)

#### Scenario: Duplicate invite refreshes via dedupe

- **WHEN** the same mobile/user already has an unread invite reminder for the same tenant under the same dedupe key
- **THEN** the system refreshes or ignores per `ImBotApi` dedupe rules instead of creating duplicate unread bot messages
