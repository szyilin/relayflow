## ADDED Requirements

### Requirement: Docs share-star API delivery

The system SHALL expose `/app-api/docs` share, shared-with-me, star, and visit-backed recent endpoints matching `openspec/lanes/workspace-docs-share-star/contract.md`, persist `doc_share` / `doc_star` / `doc_user_visit`, authorize document read/write via owner or share ACL, and best-effort notify grantees via `docs-bot`.

#### Scenario: Compile

- **WHEN** `./mvnw -pl relayflow-server -am compile` runs after this change
- **THEN** the build succeeds
