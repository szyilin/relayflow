## ADDED Requirements

### Requirement: Field group-by presentation on workspace tasks

When the active ViewConfig `groupBy` mode is `FIELD`, the `/app/tasks` list and board MUST partition root tasks by that field's value. Empty/missing values MUST appear under「无分组». Dragging a task to another partition MUST update the grouped field in the UI (temporary client-side until group-by-field API integrate). When `groupBy` is null, the list MUST render flat and the board MAY show a single「全部」column.

#### Scenario: Board columns from status groupBy

- **WHEN** groupBy is status and displayMode is BOARD
- **THEN** columns reflect status buckets including empty as「无分组」only if applicable
- **AND** dragging a card to another status column updates the card's status in the current session

#### Scenario: List sections from dueTime groupBy

- **WHEN** groupBy is dueTime and displayMode is LIST
- **THEN** tasks are shown under date section headers with「无分组」for missing due dates
