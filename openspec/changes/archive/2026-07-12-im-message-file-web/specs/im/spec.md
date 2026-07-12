## ADDED Requirements

### Requirement: Workspace file and image message UI

The workspace `/app/messages` page SHALL allow users to attach files or images to conversations and render file/image content blocks in the message list.

#### Scenario: Send image from attachment button

- **WHEN** the user selects an image file via the attachment control in an active conversation
- **THEN** the client uploads the file via infra presigned upload
- **AND** sends an IM message with `type=image` and a `file` content block referencing `fileId`
- **AND** the message list renders an image preview bubble

#### Scenario: Send generic file attachment

- **WHEN** the user selects a non-image file via the attachment control
- **THEN** the client sends `type=file` with filename metadata in content blocks
- **AND** the UI shows a file card with download affordance

#### Scenario: File message mock fallback

- **WHEN** file message REST validation is not yet implemented
- **THEN** the store uses mock messages for file/image send
- **AND** text messages continue using real APIs without page-level mock imports
