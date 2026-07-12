## ADDED Requirements

### Requirement: File message end-to-end integration

The workspace file and image message flow SHALL use real infra upload and IM APIs without mock fallbacks after integration.

#### Scenario: Integrated file send in direct chat

- **WHEN** the user sends an attachment in a direct conversation with backend APIs available
- **THEN** the message is persisted via REST and visible to both participants without mock data
