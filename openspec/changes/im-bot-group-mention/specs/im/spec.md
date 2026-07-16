## ADDED Requirements

### Requirement: Group bot mention ingress

After a user message is persisted in a group that has bot members, if the message content mentions one or more of those bots (structured `mention` blocks are the source of truth), the system MUST invoke Bot Ingress for each mentioned bot that is an active group member. Ingress MUST hand off to Bot Runtime dispatch. The system MUST NOT push a human-client realtime envelope targeted at the bot subject. Persistence of the user message MUST NOT be rolled back if Ingress/Runtime fails (best-effort inbound).

#### Scenario: Mention triggers ingress

- **GIVEN** a group with bot X as a member
- **AND** a user message whose content mentions bot X
- **WHEN** the message is persisted successfully
- **THEN** Bot Ingress is invoked for bot X
- **AND** Bot Runtime dispatch is attempted
- **AND** the system MUST NOT push a human-client WS envelope to the bot

#### Scenario: Mention of non-member bot is ignored

- **GIVEN** a message mentions bot Y that is not an active member of the group
- **WHEN** the message is persisted
- **THEN** Ingress MUST NOT dispatch Runtime for bot Y as a group mention
- **AND** the user message remains visible to human members

#### Scenario: Ingress failure does not drop user message

- **GIVEN** Bot Runtime throws or times out after a valid mention
- **WHEN** inbound handling fails
- **THEN** the original user message remains persisted
- **AND** the failure is logged server-side

## MODIFIED Requirements

### Requirement: Group bot membership and mention (phased)

The system MUST allow bots as group conversation members (see group bot membership management). When a user @mentions a bot in a group, after message persistence the system MUST invoke Bot Ingress and dispatch via Bot Runtime by `handler_kind`. Outbound `ImBotApi` MUST NOT be blocked on unfinished Runtime product handlers.

#### Scenario: Attach bot to group

- **GIVEN** a group conversation and an enabled bot
- **WHEN** an authorized action adds the bot as a member
- **THEN** `im_conversation_member` records the bot member
- **AND** member/list APIs can distinguish bots from users

#### Scenario: Mention triggers ingress

- **GIVEN** a group with a bot member and a user message mentioning that bot
- **WHEN** the message is persisted
- **THEN** Bot Ingress/Runtime is invoked
- **AND** the system MUST NOT push a human-client WS envelope to the bot

#### Scenario: noop inbound

- **GIVEN** a bot with `handler_kind=noop`
- **WHEN** a user messages its `bot_dm` or @mentions it
- **THEN** Runtime MAY produce no reply
- **AND** Outbound `ImBotApi.send` for that bot MUST still work
