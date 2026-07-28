## ADDED Requirements

### Requirement: Scheduled agent automation with bot push (skeleton)

The system SHALL allow configuring automations that run an Agent on a schedule and push results via the Agent's carrier bot to configured IM targets. Automation result delivery MUST use the IM bot channel and MUST NOT be required to append to an Agent workspace session.

#### Scenario: Cron run pushes via bot

- **WHEN** a scheduled automation fires for an Agent with a carrier bot
- **THEN** the system runs the Agent runtime for the automation prompt
- **AND** delivers the result through `ImBotApi` / carrier bot
- **AND** `/app/messages` can show that delivery while Agent session history is unchanged unless explicitly logged elsewhere
