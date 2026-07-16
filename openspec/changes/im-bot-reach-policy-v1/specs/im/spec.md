## MODIFIED Requirements

### Requirement: Bot reachability for ImBotApi.send

The system MUST classify bots with a catalog field `type`. When `type` is `system`, `ImBotApi.send` MUST deliver to the target user in a valid tenant delivery context **without** requiring rows in `im_bot_tenant_enablement` or `im_bot_user_enablement`. When `type` is not `system`, delivery MUST be allowed if **either** the tenant has an enabled subscription for the bot **or** the user has a subscription for the bot in that tenant (union). The system MUST NOT require both subscription layers simultaneously. The system MUST NOT require copying tenant subscriptions into user subscription rows on membership activation for `system` bots.

#### Scenario: System bot delivers without subscriptions

- **GIVEN** a seeded bot with `type=system` (e.g. `org-assistant`)
- **AND** no `im_bot_tenant_enablement` and no `im_bot_user_enablement` for target `(tenant, user, bot)`
- **WHEN** a caller invokes `ImBotApi.send` with a valid SINGLE or fanout target for an ACTIVE membership tenant
- **THEN** the message is persisted in a `bot_dm` and realtime push is attempted

#### Scenario: Non-system bot allowed by tenant subscription alone

- **GIVEN** a non-system bot and an enabled `im_bot_tenant_enablement` for the tenant
- **AND** no `im_bot_user_enablement` for the user
- **WHEN** `ImBotApi.send` targets that `(tenant, user)`
- **THEN** delivery succeeds

#### Scenario: Non-system bot allowed by user subscription alone

- **GIVEN** a non-system bot and an `im_bot_user_enablement` for `(tenant, user, bot)`
- **AND** no tenant enablement row
- **WHEN** `ImBotApi.send` targets that `(tenant, user)`
- **THEN** delivery succeeds

#### Scenario: Non-system bot denied when neither subscription exists

- **GIVEN** a non-system bot with neither tenant nor user subscription for the target
- **WHEN** `ImBotApi.send` is invoked
- **THEN** the send fails with a bot-not-enabled style error (for callers that do not swallow errors)

### Requirement: System bots are platform catalog defaults

Seeded platform assistants used for organization, task, approval, and account-security reach MUST be stored as `type=system` and MUST remain deliverable to all enterprises without per-tenant seed rows for enablement.

#### Scenario: New tenant receives org-assistant without seed enablement

- **GIVEN** a newly registered tenant with no bot enablement seed rows
- **WHEN** business code sends via `org-assistant` to a user with an ACTIVE membership in that or another fanout tenant
- **THEN** delivery is not blocked by missing tenant enablement for `org-assistant`
