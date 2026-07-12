## ADDED Requirements

### Requirement: Default tenant protection

The system SHALL prevent deletion of the seeded default tenant (`id=1` by default).

#### Scenario: Assert default tenant not deletable

- **WHEN** application code calls tenant deletion guard for the configured default tenant id
- **THEN** the system rejects with a business error
- **AND** the default tenant record remains
