## ADDED Requirements

### Requirement: View config uses server persistence

The `/app/tasks` view toolbar MUST load and save ViewConfig via `GET/PUT /app-api/task/view-config/*`. The client MUST NOT use `USE_LOCAL_VIEW_CONFIG` localStorage as the source of truth after integrate.

#### Scenario: Persist sort via API

- **WHEN** the user changes sort on「我负责的」and reloads with backend available
- **THEN** the saved sort is restored from the server
