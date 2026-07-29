## ADDED Requirements

### Requirement: Forward single message

The system SHALL allow a conversation member to forward exactly one non-recalled, non-`card` message to another conversation in the same tenant via `POST /app-api/im/message/forward` with `{ messageId, targetConversationId, clientMsgId? }`. The caller MUST be a member of the source conversation and allowed to send in the target conversation under existing membership rules.

#### Scenario: Successful forward

- **WHEN** a member forwards an eligible message to a permitted target conversation
- **THEN** the system inserts a new `im_message` in the target with a new `seq`
- **AND** copies content blocks (file blocks keep the same `fileId`)
- **AND** attaches `forwardedFrom` metadata with source conversation id, message id, sender nickname, and preview
- **AND** online User members of the target receive `domain=im, type=message.new`

#### Scenario: Reject recalled source

- **WHEN** the source message is recalled
- **THEN** the system MUST reject the forward

#### Scenario: Reject card message

- **WHEN** the source message `type` is `card`
- **THEN** the system MUST reject the forward

#### Scenario: Reject non-member target

- **WHEN** the caller is not allowed to send in the target conversation
- **THEN** the system MUST reject the forward
- **AND** MUST NOT insert a message

### Requirement: Forward is not a reply

Forwarded messages MUST NOT set `reply_to_msg_id` to the source message. Clients MUST render the forwarded banner from `forwardedFrom`, not from the reply quote UI.

#### Scenario: Forwarded item shape

- **WHEN** the client receives a forwarded message in list or WS
- **THEN** `replyTo` is absent or unrelated
- **AND** `forwardedFrom` is present with preview fields

### Requirement: Forward picker UI

The workspace MUST provide a forward action on the message context menu that opens a conversation picker limited to conversations the member can send to. After success, the client MUST toast success; navigating to the target conversation is optional.

#### Scenario: Menu opens picker

- **WHEN** the member chooses「转发」on an eligible message
- **THEN** a conversation picker is shown
- **AND** confirming a target invokes the forward API
