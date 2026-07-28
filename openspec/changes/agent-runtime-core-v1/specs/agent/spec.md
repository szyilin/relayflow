## ADDED Requirements

### Requirement: Agent definition CRUD for members

Authenticated members SHALL be able to create, update, list, and soft-delete their own Agent definitions in the current tenant. Each definition MUST include name, system prompt, and a `modelRef` that references an enabled `agent_llm_connection.code`. Provider secrets MUST NOT be stored on the definition.

#### Scenario: Create agent with modelRef

- **WHEN** a member creates an Agent with prompt and an enabled modelRef
- **THEN** the definition is persisted owned by that member
- **AND** raw API keys are not stored on the definition

#### Scenario: Reject unknown modelRef

- **WHEN** a member creates an Agent with a modelRef that is missing or disabled
- **THEN** the system rejects the request with a clear error

### Requirement: Agent session chat via Spring AI runtime

Members SHALL be able to create sessions against an Agent they can access, send user messages, and receive assistant replies produced by a Spring AI-backed Agent runtime. Session turns MUST be stored only in agent session tables and MUST NOT write `im_message`.

#### Scenario: Send message stores session turns only

- **WHEN** a member sends a message in an Agent session
- **THEN** the user and assistant messages are persisted in the agent session message store
- **AND** no `im_message` row is required for that turn

#### Scenario: Runtime uses organization LLM connection

- **WHEN** a session turn runs
- **THEN** the runtime resolves the Agent's modelRef to the tenant LLM connection
- **AND** invokes Spring AI chat completion using that connection's endpoint and decrypted key

### Requirement: AgentBuilder assembles prompt and tools hook

Before each run (or at session bind time), the system SHALL assemble the effective system prompt from a platform base template plus the Agent persona prompt, and SHALL attach the Agent's bound skill tools when present (empty allowlist is allowed).

#### Scenario: Persona appended to platform base

- **WHEN** an Agent with a non-empty system prompt runs
- **THEN** the model receives a system message containing platform base guidance and the persona content
