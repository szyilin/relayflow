## ADDED Requirements

### Requirement: Agent domain boundary

The system SHALL treat enterprise Agents as a dedicated domain (`agent`), separate from IM message storage. Agent configuration, agent chat sessions, and agent runtime execution MUST NOT write business chat history into `im_message` solely because a user chats in the Agent workspace.

#### Scenario: Agent workspace chat stays in agent session

- **WHEN** a member sends a message in the Agent workspace UI to an Agent
- **THEN** the system persists that turn in an Agent session store
- **AND** the system MUST NOT create a corresponding `/app/messages` timeline item for that turn unless an explicit Bot delivery path is invoked

### Requirement: Agent definition composition

The system SHALL represent a configurable Agent as a definition composed of at least: display identity, system prompt (persona), a model reference to an organization-enabled model connection, and a set of bound skills. API keys for model providers MUST NOT be stored on the Agent definition.

#### Scenario: Agent references model without embedding secrets

- **WHEN** a user configures an Agent model
- **THEN** the definition stores a `modelRef` (or equivalent reference) to an admin-managed connection
- **AND** raw provider secrets are not persisted on the Agent definition row

### Requirement: Runtime uses Spring AI behind a stable port

The system SHALL execute Agent turns through an Agent runtime port whose primary LLM and tool-calling implementation is Spring AI. Application controllers and non-runtime services MUST NOT call provider SDKs directly for Agent chat.

#### Scenario: Chat goes through AgentRuntime

- **WHEN** the workspace requests an Agent completion for a session
- **THEN** the call is handled by the Agent runtime abstraction
- **AND** Spring AI ChatClient / tool-calling is used inside that runtime implementation

### Requirement: Channel delivery separation

The system SHALL distinguish delivery channels for Agent outputs. At minimum it SHALL support an Agent workspace channel (session messages) and an IM Bot channel (messages visible under `/app/messages`). Replies MUST return to the originating channel unless a feature explicitly requests cross-channel delivery (e.g. automation push).

#### Scenario: Automation push uses IM Bot channel only

- **WHEN** an automation run completes and is configured to notify a user or group
- **THEN** delivery uses the IM Bot / `ImBotApi` path
- **AND** the automation result is not required to appear as an Agent workspace session message

### Requirement: Skills as tools calling domain APIs

The system SHALL expose bound skills to the runtime as tools. Built-in skill implementations MUST invoke other domains only through `*-api` modules and MUST NOT depend on other domains' `*-biz` modules. Skill execution MUST run under the acting member's authorization context (no silent privilege escalation to a superuser bot identity).

#### Scenario: Built-in skill uses domain API only

- **WHEN** a skill implementation needs task or calendar data
- **THEN** it calls the corresponding `*-api`
- **AND** it does not access foreign-domain mappers or `*-biz` types

### Requirement: Carrier bot lifecycle follows Agent

When a product Agent requires an IM carrier bot, the IM module SHALL provide a provisioning API usable by the Agent domain. If the Agent is removed, the associated carrier bot MUST be retired according to IM rules. Custom carrier bots exist to serve Agents; they are not a parallel notification bus.

#### Scenario: Deleting agent retires carrier bot

- **WHEN** an Agent that owns a carrier bot is deleted
- **THEN** the system retires or disables that bot via the IM provisioning API
- **AND** no orphan tenant bot remains bound to the deleted Agent definition

### Requirement: Staged delivery via child changes

Platform delivery of Agent capabilities MUST be split across dedicated OpenSpec changes (module scaffold, LLM provider admin, runtime core, workspace UI, skills, carrier bot, automation). A single change MUST NOT implement the entire Agent product surface.

#### Scenario: Mother change does not ship full product

- **WHEN** `agent-platform-foundation` is completed
- **THEN** architecture docs and capability skeleton are in place and child changes are opened
- **AND** full Agent chat UI + runtime + automation need not all be implemented inside the mother change
