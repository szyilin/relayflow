## MODIFIED Requirements

### Requirement: List-local groups on list view (web)

When the user views a task list and ViewConfig `groupBy` is not a field group (`null` or `LIST_GROUP`), the list and board MUST partition tasks by list-local groups loaded from `/app-api/task/list-group`. Create, delete, and drag MUST persist via the list-group APIs. The client MUST NOT use an in-memory-only mock flag for the production path. Field `groupBy` MUST take precedence over list-local groups (D7).

#### Scenario: Persist across refresh

- **WHEN** the user creates a list group and refreshes the page
- **THEN** the group still appears after reloading list-group list for that listId

#### Scenario: Field grouping wins

- **WHEN** ViewConfig `groupBy.mode` is `FIELD` while viewing a list
- **THEN** partitioning uses field buckets, not list-local groups
