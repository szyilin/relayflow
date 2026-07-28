## ADDED Requirements

### Requirement: Bindable skill catalog (skeleton)

The system SHALL maintain a catalog of skills that can be bound to an Agent. At runtime, bound skills SHALL be exposed as tools to the Spring AI agent loop. Built-in skill implementations MUST call other domains only via `*-api` under the acting user's authorization.

#### Scenario: Bound skill available as tool

- **WHEN** an Agent has a built-in skill bound
- **AND** a session turn runs
- **THEN** the runtime may invoke that skill as a tool
- **AND** the implementation does not depend on foreign `*-biz` modules
