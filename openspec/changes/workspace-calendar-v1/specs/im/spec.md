## ADDED Requirements

### Requirement: calendar-bot platform seed

The system MUST seed a platform bot with code `calendar-bot` as `type=system` (calendar assistant) alongside existing assistants such as `org-assistant` and `task-bot`. System-type bots MUST remain reachable via `ImBotApi.send` without tenant/user enablement rows.

#### Scenario: Seed present

- **WHEN** the platform bot catalog is migrated for calendar V1
- **THEN** a bot row with `code=calendar-bot` and `type=system` exists

#### Scenario: Send without subscription

- **WHEN** `calendar-biz` calls `ImBotApi.send` with `botCode=calendar-bot` and a valid SINGLE target
- **THEN** delivery proceeds without requiring `im_bot_tenant_enablement` or `im_bot_user_enablement`
