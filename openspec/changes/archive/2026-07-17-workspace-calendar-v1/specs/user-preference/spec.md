## ADDED Requirements

### Requirement: Calendar section in user preference

The user preference document MUST include a `settings.calendar` object for per-tenant-member calendar UI defaults. Keys MUST be merged with code defaults on GET (C-class). Changing these defaults MUST NOT rewrite existing `cal_event` reminder snapshots.

#### Scenario: Default calendar settings

- **WHEN** a member has no preference row or no `calendar` keys
- **THEN** GET preference returns code defaults including at least: `weekStartsOn`, `defaultEventDurationMinutes`, `defaultRemindBeforeMinutes`, `allDayRemindTime`, `dimPastEvents`

#### Scenario: Save calendar settings

- **WHEN** a member updates `settings.calendar` via PUT preference
- **THEN** the values persist for `(tenant_id, user_id)`
- **AND** other settings sections (e.g. `general`, `im`) remain intact under deep merge rules

### Requirement: Settings panel Calendar category

The workspace global settings panel (opened from the profile/avatar entry) MUST expose a **日历** category for editing `settings.calendar`. Calendar preferences MUST NOT use a separate page-local settings store as the source of truth.

#### Scenario: Open calendar category

- **WHEN** a user opens Settings and selects 日历
- **THEN** they can edit week start, default event duration, and default reminder fields
- **AND** saving uses the user-preference API (or store that targets it after integrate)
