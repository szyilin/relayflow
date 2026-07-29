## ADDED Requirements

### Requirement: Feishu-style emoji and sticker picker

The workspace composer MUST open a picker with bottom tabs for **system emoji**, **custom stickers**, and **settings**. The composer trigger icon MUST use a line-art smile (not the sticker/document glyph).

#### Scenario: Open picker

- **WHEN** the member clicks the smile icon on the composer
- **THEN** the picker opens with the system emoji tab as default

### Requirement: System emoji with recently used

The system emoji tab MUST show a **最近使用** section of at most two rows (12 glyphs at 6 columns), ordered by most recently selected first, then the full static system emoji catalog in fixed order. Selecting a system emoji MUST insert it into the composer draft (not send a sticker message) and MUST update the recently-used list.

#### Scenario: Recent reorder

- **WHEN** the member selects an emoji that was already in recently used
- **THEN** that emoji moves to the first position of 最近使用

### Requirement: Custom sticker tab and settings

The custom tab MUST list the member's image/GIF stickers and provide an add affordance (empty state or leading cell). Selecting a custom sticker MUST send it as a sticker message. The settings tab MUST support reorder and batch delete of custom stickers.

#### Scenario: Empty custom tab

- **WHEN** the member has no custom stickers
- **THEN** the tab shows a click-to-add empty state

## MODIFIED Requirements

### Requirement: Sticker picker in composer

The workspace composer MUST provide an entry to open a Feishu-style emoji/sticker picker (system emoji catalog client-side; custom stickers via API). Selecting a **custom** sticker MUST send it as a sticker message without requiring text. Selecting a **system emoji** MUST insert into the draft.

#### Scenario: Send custom sticker from picker

- **WHEN** the member selects a custom sticker from the picker
- **THEN** the client sends a sticker message to the active conversation
- **AND** the bubble renders the sticker image
