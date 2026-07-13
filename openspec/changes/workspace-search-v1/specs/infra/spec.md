## ADDED Requirements

### Requirement: Workspace search REST endpoint

The system SHALL register the workspace search aggregation endpoint under `/app-api/infra/workspace-search` in `infra-biz`.

#### Scenario: Infra hosts aggregation route

- **WHEN** workspace search is enabled
- **THEN** clients call `/app-api/infra/workspace-search` rather than ad-hoc multi-fetch orchestration in the browser
- **AND** the handler delegates to `system-api`, `im-api`, and `task-api` contracts
