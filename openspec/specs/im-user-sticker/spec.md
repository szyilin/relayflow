# im-user-sticker Specification

## Purpose

Per-user custom sticker library: list, upload/collect, reorder/delete, and send as sticker messages.

## Requirements

### Requirement: List my custom stickers

The system SHALL expose `GET /app-api/im/sticker/mine` returning the current user's custom stickers ordered by `sort` ascending, each with `id`, `fileId`, resolvable `downloadUrl`, and optional `name`.

#### Scenario: Member lists custom stickers

- **WHEN** an authenticated workspace member requests their custom stickers
- **THEN** the response contains only stickers owned by that user in the current tenant

### Requirement: Add custom sticker from upload

The system SHALL accept `POST /app-api/im/sticker/mine` with `{ fileId, name? }` after the file is stored via infra upload. The file MIME MUST be an image type (including `image/gif`). Video and non-image types MUST be rejected. Each user MUST NOT exceed 200 custom stickers.

#### Scenario: Add image sticker

- **WHEN** a member posts a valid image `fileId`
- **THEN** a new `im_user_sticker` row is created and returned

#### Scenario: Reject video

- **WHEN** `fileId` points to a non-image MIME
- **THEN** the API returns a business error and does not create a row

### Requirement: Add custom sticker from sticker message

The system SHALL accept `POST /app-api/im/sticker/mine/from-message` with `{ messageId }`. The message MUST be visible to the caller, `type=sticker` (or contain a sticker block), and resolve to an image `fileId`. The server MUST copy that `fileId` into the caller's custom list (dedupe by fileId optional: if already owned, return existing).

#### Scenario: Collect from peer sticker

- **WHEN** a member collects a visible sticker message that has a file
- **THEN** the sticker appears in their custom list

#### Scenario: Reject non-sticker message

- **WHEN** `messageId` is text or other non-sticker type
- **THEN** the API rejects the request

### Requirement: Reorder and batch delete custom stickers

The system SHALL accept `PUT /app-api/im/sticker/mine/reorder` with ordered `ids` owned by the caller, rewriting `sort`. The system SHALL accept `DELETE /app-api/im/sticker/mine` with `ids` to soft-delete those rows.

#### Scenario: Batch delete

- **WHEN** a member deletes a subset of their sticker ids
- **THEN** those stickers no longer appear in `GET /mine`

### Requirement: Send custom sticker message

The system SHALL allow send with `type=sticker` and `userStickerId` referencing a visible owned custom sticker. Persistence MUST store a sticker content block with `userStickerId`, `fileId`, and downloadable URL for peers.

#### Scenario: Send owned custom sticker

- **WHEN** a member sends their custom sticker in a writable conversation
- **THEN** peers receive `message.new` with sticker type and image URL
