## ADDED Requirements

### Requirement: Agent Maven module on server classpath

The V1 runtime SHALL include `relayflow-module-agent-api` and `relayflow-module-agent-biz` in the Maven reactor. `relayflow-server` MUST depend on `agent-biz`. Spring AI dependency versions MUST be managed only via the RelayFlow BOM importing `spring-ai-bom` 1.1.x (compatible with Spring Boot 3.4), and MUST NOT require Spring AI 2.x / Boot 4.

#### Scenario: Server compiles with agent-biz

- **WHEN** the agent module scaffold is applied
- **THEN** `./mvnw -pl relayflow-server -am compile` succeeds with `agent-biz` on the classpath
- **AND** the BOM declares Spring AI 1.1.x management without requiring `spring.ai.openai.api-key` at application bootstrap for the scaffold alone

### Requirement: Agent package layout placeholder

The `agent-biz` module SHALL reserve Java packages for definition, builder, runtime, skill, session, channel, and llm concerns so later Agent slices place code consistently.

#### Scenario: Placeholder packages exist

- **WHEN** the scaffold change is complete
- **THEN** `com.relayflow.module.agent` contains documented placeholder packages (or package-info) for the listed concerns
