## ADDED Requirements

### Requirement: Agent carrier bot provisioning API (skeleton)

The IM module SHALL expose a provisioning API so the Agent domain can create and retire tenant carrier bots bound to an Agent. Deleting an Agent MUST retire its carrier bot. Inbound messages to a carrier bot MAY be dispatched to the Agent runtime and replied in the same IM conversation without requiring those turns to appear in the Agent workspace session store.

#### Scenario: Provision bot when agent needs carrier

- **WHEN** the Agent domain requests provisioning for an Agent carrier
- **THEN** IM creates or ensures a non-system bot bound to that Agent
- **AND** the Agent stores the bot identifier as its carrier reference
