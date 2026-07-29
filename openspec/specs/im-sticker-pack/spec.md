# im-sticker-pack Specification

## Purpose

System sticker pack catalog and sticker message send/picker.

## Requirements

### Requirement: Sticker pack catalog

The system SHALL expose `GET /app-api/im/sticker/packs` returning sticker packs visible to the current tenant member. V1 MUST include at least the seeded **system** pack (`scope=system`). Each pack MUST list stickers with `id`, `name`/`code`, and a resolvable image URL (via `infra_file`).

#### Scenario: List system pack

- **WHEN** an authenticated workspace member requests sticker packs
- **THEN** the response includes the system pack and its stickers with downloadable URLs

### Requirement: Send sticker message

The system SHALL accept send requests with `type=sticker` and a valid `stickerId` belonging to a visible pack. Persistence MUST store `im_message.type=sticker` and a `content_json` block `type=sticker` referencing `stickerId`, `packId`, and `fileId`. Online peers MUST receive `message.new`.

#### Scenario: Send sticker in conversation

- **WHEN** a member sends a sticker in a conversation they can write to
- **THEN** the message is persisted with `type=sticker`
- **AND** list/WS payloads include enough data for the client to render the sticker image

#### Scenario: Reject unknown sticker

- **WHEN** `stickerId` is missing or not visible to the tenant
- **THEN** the system MUST reject the send

### Requirement: Sticker picker in composer

The workspace composer MUST provide an entry to open a Feishu-style emoji/sticker picker (system emoji catalog client-side; custom stickers via API). Selecting a **custom** sticker MUST send it as a sticker message without requiring text. Selecting a **system emoji** MUST insert into the draft.

#### Scenario: Send custom sticker from picker

- **WHEN** the member selects a custom sticker from the picker
- **THEN** the client sends a sticker message to the active conversation
- **AND** the bubble renders the sticker image

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

### Requirement: Tenant custom packs deferred

V1 MUST NOT require tenant-uploaded sticker packs or an admin upload UI. Schema MAY reserve `scope=tenant` for a later change.

#### Scenario: No upload surface

- **WHEN** a member uses the workspace sticker picker
- **THEN** only catalog stickers from available packs are shown
- **AND** no upload control is required in this change
