## ADDED Requirements

### Requirement: Task view config toolbar on workspace tasks

The `/app/tasks` main pane (except「动态」) SHALL provide a view toolbar to edit `displayMode`, `groupBy`, `sort`, overlay filters, and visible field keys for the current view context. Changing these settings MUST update the active ViewConfig for that context. Until the view-config API is integrated, persistence MAY use store-local storage keyed by tenant and user; LIST shared-server semantics are deferred to `-api`.

#### Scenario: Persist sort for mine

- **WHEN** the user sets sort to due time on「我负责的」and refreshes the page
- **THEN** the toolbar still shows due-time sort for that user on「我负责的」

#### Scenario: Contexts are independent

- **WHEN** the user sets groupBy on「全部任务」to status
- **THEN**「我负责的」groupBy remains unchanged unless separately edited
