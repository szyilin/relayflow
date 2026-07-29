## ADDED Requirements

### Requirement: Add sticker message to my custom stickers

The message context menu (and equivalent overflow menu) MUST show **添加到表情** only when the target message is a sticker message (`type=sticker` or sticker content block) that is not recalled. Choosing it MUST call the collect-from-message API and refresh the custom sticker list.

#### Scenario: Sticker message shows collect action

- **WHEN** the member opens the context menu on a sticker message
- **THEN** 添加到表情 is present

#### Scenario: Text message hides collect action

- **WHEN** the member opens the context menu on a text message
- **THEN** 添加到表情 is not present
