## MODIFIED Requirements

### Requirement: Member invite notification trigger

The system SHALL attempt to deliver a user-visible invite reminder via `ImBotApi.send` with bot code `org-assistant` when an administrator successfully invites a member by mobile. Invite persistence (membership / dept / roles) MUST succeed independently of Bot delivery. If `ImBotApi.send` fails, the invite API MUST still return success and MUST NOT expose Bot enablement or delivery errors to the client.

#### Scenario: Invite succeeds even when bot delivery fails

- **WHEN** `POST /admin-api/system/user/invite` creates a `NOT_JOINED` membership
- **AND** `ImBotApi.send` throws or returns a delivery failure
- **THEN** the invite API response is still success (`code=0`)
- **AND** the client MUST NOT receive a Bot-not-enabled error for that invite

#### Scenario: Invite still pushes org-assistant when reachable

- **WHEN** invite succeeds and the invitee has at least one `ACTIVE` membership
- **AND** Bot delivery is reachable under IM reach rules
- **THEN** the system attempts `ImBotApi.send` with `botCode=org-assistant` and `ALL_ACTIVE_MEMBERSHIPS`
- **AND** on success the reminder is persisted as `im_message` with `sender_type=bot`
