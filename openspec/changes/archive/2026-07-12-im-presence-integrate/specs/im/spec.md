## ADDED Requirements

### Requirement: Presence end-to-end integration

The workspace presence indicators SHALL use real batch presence APIs without mock fallbacks after integration.

#### Scenario: Integrated online indicator

- **WHEN** two users in the same tenant are viewing contacts or direct chat asides
- **THEN** online status reflects actual WebSocket session state from the presence batch API
