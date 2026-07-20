## ADDED Requirements

### Requirement: Docs Library V1 schema tables

The system SHALL provide Flyway migrations creating `doc_object` and `doc_library_node` with `doc_` prefix and tenant columns. Library V1 schema MUST NOT require `doc_embed`.

#### Scenario: Core tables exist after migrate

- **WHEN** docs Library V1 schema migrations are applied
- **THEN** tables `doc_object` and `doc_library_node` exist
- **AND** `doc_object` stores `body` as JSONB with a `body_format` column
- **AND** table `doc_embed` is not required

#### Scenario: Docs Maven module loads

- **WHEN** `relayflow-server` is built with docs-biz on the classpath
- **THEN** module `relayflow-module-docs-api` and `relayflow-module-docs-biz` compile successfully
