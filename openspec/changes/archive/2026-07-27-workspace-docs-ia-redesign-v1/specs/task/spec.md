## ADDED Requirements

### Requirement: Task assignee uses owner chip

Workspace task surfaces that show a single assignee or assignee set MUST render each assignee with the shared owner/user chip (avatar + display name) that opens the personal business card on click, instead of plain text nicknames or the bare label「我」alone.

#### Scenario: List row assignee chip

- **WHEN** a task row displays an assignee
- **THEN** the UI uses the owner chip component with avatar and display name

#### Scenario: Multi-assignee overflow

- **WHEN** a task has more assignees than the visible chip budget
- **THEN** the UI shows a limited number of chips plus an overflow indicator
- **AND** visible chips remain individually clickable
