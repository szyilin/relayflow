## MODIFIED Requirements

### Requirement: Personal custom groups on mine inbox (web)

When the user views「我负责的」with ViewConfig `groupBy.mode=PERSONAL_CUSTOM`, the list and board MUST partition tasks by personal groups loaded from `/app-api/task/mine-group`. Create, delete, and drag MUST persist via the mine-group APIs. The client MUST NOT use an in-memory-only mock flag for production path.

#### Scenario: Persist across refresh

- **WHEN** the user creates a personal group and refreshes the page
- **THEN** the group still appears after reloading mine-group list
