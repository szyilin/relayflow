## ADDED Requirements

### Requirement: docs-bot platform seed

The system MUST seed a platform bot with code `docs-bot` as `type=system` (document assistant) alongside existing assistants such as `org-assistant`, `task-bot`, and `calendar-bot`. System-type bots MUST remain reachable via `ImBotApi.send` without requiring tenant/user enablement rows for delivery authorization.

#### Scenario: Seed present

- **WHEN** the platform bot catalog is migrated for docs share V1
- **THEN** a bot row with `code=docs-bot` and `type=system` exists

#### Scenario: Send without subscription

- **WHEN** `docs-biz` calls `ImBotApi.send` with `botCode=docs-bot` and a valid SINGLE target
- **THEN** delivery proceeds without requiring `im_bot_tenant_enablement` or `im_bot_user_enablement` for authorization
