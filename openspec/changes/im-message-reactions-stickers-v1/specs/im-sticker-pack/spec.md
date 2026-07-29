## ADDED Requirements

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

### Requirement: Sticker picker UI

The workspace composer MUST provide an entry to open a sticker picker backed by the pack catalog (Mock in `-web`, real API after integrate). Selecting a sticker MUST send it as a sticker message without requiring text.

#### Scenario: Pick and send

- **WHEN** the member selects a sticker from the picker
- **THEN** the client sends a sticker message to the active conversation
- **AND** the bubble renders the sticker image

### Requirement: Tenant custom packs deferred

V1 MUST NOT require tenant-uploaded sticker packs or an admin upload UI. Schema MAY reserve `scope=tenant` for a later change.

#### Scenario: No upload surface

- **WHEN** a member uses the workspace sticker picker
- **THEN** only catalog stickers from available packs are shown
- **AND** no upload control is required in this change
