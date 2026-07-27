## ADDED Requirements

### Requirement: Docs IA list field API

Docs list APIs MUST expose fields required by the home-ia contract for Home / Library / Drive tables including owner identity suitable for owner chips.

#### Scenario: Contract fields present

- **WHEN** client calls listed endpoints in the lane contract
- **THEN** responses include the documented owner and time fields
