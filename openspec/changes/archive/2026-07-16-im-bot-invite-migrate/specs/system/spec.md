## MODIFIED Requirements

### Requirement: Member invite notification trigger

The system SHALL deliver a user-visible invite reminder via `ImBotApi.send` with bot code `org-assistant` when an administrator successfully invites a member by mobile. The system MUST NOT write `infra_notify`, call `NotifyInboxApi`, or use bot code `invite-helper`.

#### Scenario: Invite pushes org-assistant bot_dm to active memberships

- **WHEN** `POST /admin-api/system/user/invite` succeeds and creates a `NOT_JOINED` membership
- **AND** the invitee already has at least one `ACTIVE` tenant membership
- **THEN** the system calls `ImBotApi.send` with `botCode=org-assistant` and target scope `ALL_ACTIVE_MEMBERSHIPS`
- **AND** the message text includes the inviting tenant name and inviter display name
- **AND** `dedupeKey` is stable per inviting tenant (e.g. `MEMBER_INVITE:{tenantId}`)

#### Scenario: Invite with no active membership skips bot delivery

- **WHEN** the invitee has no `ACTIVE` memberships
- **THEN** the invite membership is still created successfully
- **AND** the system MUST NOT fail the invite solely because no bot_dm could be delivered
- **AND** pending invite preview / registration banner remains available

#### Scenario: Duplicate invite refreshes via dedupe

- **WHEN** the same mobile/user already has an in-window invite reminder for the same inviting tenant under the same dedupe key
- **THEN** the system refreshes or ignores per `ImBotApi` dedupe rules instead of creating duplicate unread bot messages
