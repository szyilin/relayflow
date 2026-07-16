## ADDED Requirements

### Requirement: Interactive card send and action SPI

The system MUST support Feishu-like interactive cards as IM message content. `ImBotApi.send` MUST accept a card document (including `generic.v1` template with header/fields/actions). Clients MUST render cards in conversation history. Action behaviors are limited to `open_url` (client-local navigation) and `callback` (server SPI, optional form values). The system MUST expose `POST /app-api/im/card/action` that authenticates the caller as a User member of the conversation, enforces `meta.expiresAt`, supports idempotency via `clientActionId` (or equivalent), and dispatches to an in-process `CardActionHandler` resolved by `actionKey`. Handlers are defined in `im-api` and implemented by business modules (or a documented demo handler). Successful callbacks MAY return toast and/or an updated card snapshot that IM patches onto the message and pushes via realtime. System bots MUST NOT require external callback URLs. The system MUST NOT dual-write `infra_notify` or use `domain=notify` as a business write model for card interactions. Bot Runtime dialogue inbound MUST remain a separate path from card actions.

#### Scenario: Send card via ImBotApi

- **GIVEN** a reachable user and bot
- **WHEN** a caller invokes `ImBotApi.send` with a card document
- **THEN** the message is persisted with card content
- **AND** online User members receive `domain=im, type=message.new`

#### Scenario: open_url does not hit action API

- **GIVEN** a card action with `behavior.type=open_url`
- **WHEN** the user activates that action
- **THEN** the client navigates using the route/URL locally
- **AND** MUST NOT call `POST /app-api/im/card/action` for that activation

#### Scenario: callback dispatches handler

- **GIVEN** a card action with `behavior.type=callback` and a registered `CardActionHandler` for its `actionKey`
- **WHEN** the user submits `POST /app-api/im/card/action` with valid membership and unexpired card
- **THEN** the handler is invoked with opaque `payload` and optional `formValues`
- **AND** the HTTP response may include toast and updated message/card data

#### Scenario: Expired card rejects interaction

- **GIVEN** `meta.expiresAt` is in the past
- **WHEN** the user calls the card action API
- **THEN** the system rejects the interaction
- **AND** MUST NOT invoke the business handler

#### Scenario: Idempotent clientActionId

- **GIVEN** a successful callback for a `clientActionId`
- **WHEN** the same caller retries with the same `clientActionId`
- **THEN** the system does not apply the business side effect twice
- **AND** returns a stable success-equivalent response

#### Scenario: No notify dual-write

- **WHEN** a card is sent or a callback completes
- **THEN** the system MUST NOT insert into `infra_notify`
- **AND** MUST NOT require `domain=notify` as the write model

## MODIFIED Requirements

### Requirement: Card content placeholder

The message content model MUST support a `card` shape for Feishu-like interactive cards. Text plus deep-link metadata remains allowed for lightweight reach. Interactive callback auth/timeout/idempotency is implemented via the interactive card action SPI and MUST NOT resurrect a parallel notify write model.

#### Scenario: Text reach allowed

- **GIVEN** `ImBotApi.send` with text and optional deep-link metadata
- **WHEN** the message is persisted
- **THEN** clients can render it in `bot_dm` and navigate the deep link

#### Scenario: Card type persists

- **GIVEN** a sender uses the `card` content type per implementation contract
- **WHEN** the message is persisted
- **THEN** `im_message` succeeds
- **AND** card interactions use Card Action Ingress rather than `infra_notify`
