## MODIFIED Requirements

### Requirement: Calendar section in user preference

The user preference document MUST include a `settings.calendar` object for per-tenant-member calendar UI defaults. Keys MUST be merged with code defaults on GET (C-class). Changing these defaults MUST NOT rewrite existing `cal_event` reminder snapshots. Defaults MUST include `showTaskLayer` (boolean, default `true`) controlling whether the calendar page initially enables the「我的任务」virtual layer. `settings.calendar` MUST NOT store per-calendar display color overrides; those MUST live in the calendar domain user-pref table (see calendar specs). Admin calendar product settings MUST NOT write `settings.calendar` personal fields.

#### Scenario: Default calendar settings

- **WHEN** a member has no preference row or no `calendar` keys
- **THEN** GET preference returns code defaults including at least: `weekStartsOn`, `defaultEventDurationMinutes`, `defaultRemindBeforeMinutes`, `allDayRemindTime`, `dimPastEvents`, `showTaskLayer`

#### Scenario: Save calendar settings

- **WHEN** a member updates `settings.calendar` via PUT preference
- **THEN** the values persist for `(tenant_id, user_id)`
- **AND** other settings sections (e.g. `general`, `im`) remain intact under deep merge rules

#### Scenario: Default showTaskLayer

- **WHEN** `showTaskLayer` is absent from a stored preference document
- **THEN** GET merges it as `true`

#### Scenario: No per-calendar colors in preference

- **WHEN** a member saves the global settings「日历」category
- **THEN** the persisted `settings.calendar` object MUST NOT contain a map of calendarId→color as the source of truth for sidebar/grid colors
