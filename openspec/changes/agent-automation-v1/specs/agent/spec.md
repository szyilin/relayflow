## ADDED Requirements

### Requirement: Scheduled agent automation with bot push

The system SHALL allow configuring automations that run an Agent on a cron schedule and push results via the Agent's carrier bot to a configured IM user target. Automation result delivery MUST use `ImBotApi` / the carrier bot channel and MUST NOT append to an Agent workspace session.

Automations SHALL store at least: title, prompt, agent id, cron expression, status, target user id, next/last run metadata. A platform XXL-JOB handler SHALL periodically execute due automations.

#### Scenario: Cron run pushes via bot

- **WHEN** a scheduled automation fires for an Agent with a carrier bot
- **THEN** the system runs the Agent runtime for the automation prompt
- **AND** delivers the result through `ImBotApi` / carrier bot
- **AND** `/app/messages` can show that delivery while Agent session history is unchanged

#### Scenario: Manual run now

- **WHEN** the owner triggers run-now for an active automation
- **THEN** the system executes the same run path as the scheduler
- **AND** still does not write `agent_session` messages
