## ADDED Requirements

### Requirement: Agent carrier bot provisioning API

The IM module SHALL expose `ImBotProvisioningApi` so the Agent domain can create, update, and retire tenant carrier bots bound to an Agent (`type=tenant`, `handler_kind=platform`, `owner_agent_id`). Creating an Agent MUST provision (or ensure) its carrier bot and store `carrier_bot_id`. Deleting an Agent MUST retire its carrier bot. Provisioning SHALL be idempotent per `agentId`.

#### Scenario: Provision bot when agent needs carrier

- **WHEN** the Agent domain requests provisioning for an Agent carrier
- **THEN** IM creates or ensures a non-system bot bound to that Agent
- **AND** the Agent stores the bot identifier as its carrier reference

#### Scenario: Retire bot when agent is deleted

- **WHEN** an Agent is deleted
- **THEN** IM retires the bound carrier bot (disabled / not dispatchable)

### Requirement: Carrier inbound to AgentRuntime

Inbound user messages to a carrier bot (group @mention of a member bot, or bot_dm) SHALL be dispatched to the Agent runtime under the triggering user's security context. The assistant reply SHALL be written as an IM bot message in the same conversation. Those turns MUST NOT be written to `agent_session`.

#### Scenario: bot_dm inbound replies in IM

- **WHEN** a user sends a text message in a bot_dm with a carrier bot
- **THEN** the Agent runtime may produce a reply
- **AND** the reply is persisted via bot reply in that conversation
- **AND** no `agent_session_message` row is created for that turn
