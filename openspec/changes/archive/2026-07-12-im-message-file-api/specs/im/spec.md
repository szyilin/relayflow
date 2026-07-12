## ADDED Requirements

### Requirement: File and image message persistence

The system SHALL accept IM messages with `type=image` or `type=file` whose `content_json` contains at least one `file` block referencing a valid tenant-scoped `infra_file.fileId`.

#### Scenario: Send image message

- **WHEN** an authenticated member sends a message with `type=image` and a file block whose mimeType starts with `image/`
- **THEN** the message is persisted with the given type and content
- **AND** conversation `lastMsgPreview` reflects an image placeholder

#### Scenario: Reject foreign file reference

- **WHEN** the fileId does not exist in the current tenant or is not accessible to the sender
- **THEN** the API rejects the send with a clear business error
- **AND** no message row is inserted

#### Scenario: List enriches download URL

- **WHEN** a member lists messages containing file blocks
- **THEN** each file block includes a client-usable download URL for the app file download endpoint
