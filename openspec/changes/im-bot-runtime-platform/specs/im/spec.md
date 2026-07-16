## ADDED Requirements

### Requirement: Bot Runtime SPI and handler kinds

The IM module MUST provide a Bot Runtime that dispatches inbound bot events by catalog `handler_kind`. V1 MUST implement `noop` (consume without reply) and `platform` (dispatch to registered in-process handlers; missing handler behaves as noop). `webhook` MAY be stubbed and MUST NOT perform external HTTP in V1. Runtime replies MUST be persisted in the same conversation with `sender_type=bot` and realtime-pushed only to User members. Card action handling MUST use a separate ingress/SPI and MUST NOT be implemented as Bot Runtime handlers. Business modules MUST NOT own group @-receive logic; they reach users via `ImBotApi.send` or domain events.

#### Scenario: noop produces no reply

- **GIVEN** a bot with `handler_kind=noop`
- **WHEN** Bot Runtime receives an inbound mention or bot_dm message
- **THEN** Runtime produces no reply message
- **AND** Outbound `ImBotApi.send` for that bot still works

#### Scenario: platform dispatches in-process handler

- **GIVEN** a bot with `handler_kind=platform` and a registered platform handler
- **WHEN** Runtime receives inbound context for that bot
- **THEN** the registered handler is invoked
- **AND** any reply is stored as `sender_type=bot` in the same conversation

#### Scenario: webhook is stub only

- **GIVEN** a bot with `handler_kind=webhook`
- **WHEN** Runtime receives inbound context
- **THEN** the system does not perform an external HTTP callback in V1
- **AND** behavior is documented as unimplemented/stub

#### Scenario: Bot members do not receive client push

- **GIVEN** a Runtime reply is persisted in a group
- **WHEN** realtime fanout runs
- **THEN** only User members may receive `domain=im` client envelopes
- **AND** Bot subjects MUST NOT be treated as login clients
