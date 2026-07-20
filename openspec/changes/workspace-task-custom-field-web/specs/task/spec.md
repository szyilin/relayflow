## ADDED Requirements

### Requirement: List custom single-select field UI (web)

Within a task list context, the workspace tasks UI MUST allow users with list edit rights to define list-scoped **single-select** custom fields (name + ordered options). Field schemas MUST NOT appear on quick-access contexts. Until custom-field API integrate, definitions and values MAY persist only in the client session (store mock).

#### Scenario: Create field and options

- **WHEN** a list OWNER/EDITOR creates a single-select field with two or more options
- **THEN** the field appears in that list's field list for the current session
- **AND** quick-access contexts MUST NOT show a list field schema editor

#### Scenario: Viewer read-only

- **WHEN** a list VIEWER opens the list
- **THEN** they MUST NOT be able to create, rename, reorder, or delete custom fields or options

### Requirement: Custom field as groupBy source (web)

When ViewConfig `groupBy.mode` is `FIELD` and `fieldKey` is `custom:{fieldId}` for a field belonging to the current list, the list and board MUST partition root tasks by that field's selected option. Empty/missing values MUST appear under「无分组」(`__empty__`). Dragging between partitions MUST update the mock value for the current session until API integrate.

#### Scenario: Board columns from custom select

- **WHEN** the user sets groupBy to a list custom single-select field
- **THEN** board columns (or list sections) match the field's options plus「无分组」for empty values

#### Scenario: Drag updates value

- **WHEN** the user drags a root task into an option column
- **THEN** that task's mock custom-field value becomes that option for the current list context

### Requirement: Detail panel custom field value (web)

In a list context, the task detail panel MUST show the current list's custom single-select fields and allow EDITOR+/OWNER to change or clear the value (clear →「无分组」). Values for other lists MUST NOT be edited unless the user switches list context.

#### Scenario: Edit value in detail

- **WHEN** an EDITOR sets a custom single-select value on a task in list L
- **THEN** that value is reflected in list L's groupBy partitions in the current session
