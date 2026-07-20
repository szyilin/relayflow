## ADDED Requirements

### Requirement: List custom field persistence API

The system SHALL persist list-scoped single-select custom fields in `task_list_field`, options in `task_list_field_option`, and task values in `task_item_field_value` (EAV). REST under `/app-api/task/list-field` MUST support list/create/update/delete field, option CRUD, and PUT value. Deleting an option MUST clear referencing values; deleting a field MUST cascade options and values without deleting tasks. `PUT /app-api/task/item/group-move` MUST accept `fieldKey=custom:{fieldId}` with required `listId` and write the matching option by `value_key` (or clear for empty).

#### Scenario: Create and list fields

- **WHEN** an OWNER/EDITOR creates a SINGLE_SELECT field with options for a list
- **THEN** `GET /list-field/list?listId=` returns that field and options for list members

#### Scenario: Custom group-move

- **WHEN** an editable user calls group-move with `fieldKey=custom:{id}`, `listId`, and an option `value_key`
- **THEN** the task's EAV value for that field is updated to the option
