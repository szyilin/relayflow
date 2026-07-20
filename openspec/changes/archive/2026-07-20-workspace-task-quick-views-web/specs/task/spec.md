## ADDED Requirements

### Requirement: Workspace quick-access navigation

The `/app/tasks` left navigation SHALL present three sections: personal entries（我负责的、我关注的、动态）, **快速访问**（全部任务、我创建的、我分配的、已完成）, and task lists. Quick-access entries MUST behave as preset query contexts (not list containers). Deep links MUST support `?view=` for these contexts without requiring `listId`.

#### Scenario: Open all tasks from nav

- **WHEN** the user clicks「全部任务」
- **THEN** the main pane title is「全部任务」
- **AND** the route includes `view=all` (or equivalent documented query)
- **AND** no `listId` is required

#### Scenario: Open assigned-by-me from nav

- **WHEN** the user clicks「我分配的」
- **THEN** the main pane title is「我分配的」
- **AND** the list shows tasks matching the assigned-by-me seed (API or temporary store data until assigner API exists)
