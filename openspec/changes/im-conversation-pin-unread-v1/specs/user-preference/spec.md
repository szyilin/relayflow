## ADDED Requirements

### Requirement: IM section in user preference for pinned collapse

The user preference document MUST include a `settings.im` object for per-tenant-member IM UI preferences. Keys MUST be merged with code defaults on GET (C-class). Defaults MUST include `pinnedSectionCollapsed` (boolean, default `false`). PUT MUST accept a boolean for this key under deep merge; absent key MUST merge to the code default. Changing this preference MUST NOT alter conversation membership, pin flags, or read watermarks.

#### Scenario: Default im settings

- **WHEN** a member has no preference row or no `im` keys
- **THEN** GET preference returns code defaults including `im.pinnedSectionCollapsed = false`

#### Scenario: Save pinned section collapsed

- **WHEN** a member updates `settings.im.pinnedSectionCollapsed` via PUT preference
- **THEN** the value persists for `(tenant_id, user_id)`
- **AND** other settings sections remain intact under deep merge rules

### Requirement: IM sidebar group visibility and order in user preference

`settings.im` MUST also support message sidebar group preferences: an ordered list of visible built-in group ids and a list of hidden built-in group ids (field names MAY be `sidebarGroupOrder` / `sidebarGroupHidden` or an equivalent single structure). Defaults MUST show all nine built-in groups of this version in product order. PUT MUST sanitize unknown ids; an empty visible list MUST merge back to the code default full set. Changing these keys MUST NOT alter conversation membership or tag assignments.

#### Scenario: Default all nine groups visible

- **WHEN** a member has no sidebar group preference keys
- **THEN** GET preference effective document includes all nine built-in group ids visible in default order

#### Scenario: Persist hidden and order

- **WHEN** a member updates sidebar group order/hidden via PUT preference
- **THEN** the values persist for `(tenant_id, user_id)` and subsequent GET reflects them after merge/sanitize
