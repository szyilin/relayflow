## ADDED Requirements

### Requirement: WebSocket JSON envelope

The system MUST use a unified JSON Envelope for WebSocket upstream and downstream messages containing `domain`, `type`, `requestId`, `ts`, and `payload`.

#### Scenario: Upstream envelope structure

- **WHEN** a client sends business data over `/infra/ws`
- **THEN** the server MUST parse `domain` and `type` for handler routing
- **AND** invalid JSON or missing `domain`/`type` MUST be rejected with an error envelope or connection close policy as documented

#### Scenario: Downstream envelope structure

- **WHEN** the server pushes an event to a client
- **THEN** the WebSocket frame MUST use the same Envelope shell
- **AND** `domain` MUST match semantics (e.g. new IM message uses `domain=im`)

### Requirement: WebSocket domain routing

The system MUST route Envelopes by `domain` to registered Domain Handlers; V1 MUST register `im` and `system` (ping/pong), and MUST register placeholder handlers for `notify` and `presence`.

#### Scenario: IM domain routing

- **WHEN** a client sends `domain=im, type=message.send`
- **THEN** the envelope MUST be routed to the IM module handler

#### Scenario: Placeholder domain ignored

- **WHEN** a client sends `domain=notify` and V1 notify handler is not implemented
- **THEN** the placeholder handler MUST ignore or log at debug level
- **AND** MUST NOT abnormally terminate the WebSocket connection

### Requirement: WebSocket tenant and user binding

The system MUST bind `tenant_id` and `user_id` to the Session after a successful handshake; delivery and online state MUST be tenant-scoped and cross-tenant push is forbidden.

#### Scenario: Handshake binds tenant

- **WHEN** JWT contains `tenant_id=1` and valid user id and handshake succeeds
- **THEN** session registration includes `tenantId=1` and `userId`
- **AND** subsequent fanout matches tenant-scoped sessions only

#### Scenario: Cross-tenant delivery blocked

- **WHEN** an instance attempts to push to `tenant_id=2`
- **THEN** sessions belonging to other tenants MUST NOT receive the message

### Requirement: RealtimeTransport contract

The system MUST expose `RealtimeTransportApi` in `infra-api` for business modules to deliver Envelopes to users; business modules MUST NOT access WebSocket SessionRegistry directly.

#### Scenario: Push to online user

- **WHEN** `RealtimeTransportApi.sendToUser(tenantId, userId, envelope)` is called and the user is online in the cluster
- **THEN** the client receives the downstream WebSocket message

#### Scenario: Online status query

- **WHEN** the Redis online key exists and is not expired
- **THEN** `RealtimeTransportApi.isUserOnline(tenantId, userId)` returns true

### Requirement: RealtimeEventPublisher contract

The system MUST expose `RealtimeEventPublisher` in `infra-api` as the unified entry for realtime domain events; the implementation delegates to handlers or no-op stubs.

#### Scenario: Publish IM domain event

- **WHEN** an IM `domain=im` RealtimeEvent is published
- **THEN** IM delivery logic or delegated IM handler is invoked

#### Scenario: Publish placeholder domain event

- **WHEN** a `domain=notify` RealtimeEvent is published in V1
- **THEN** the call completes successfully
- **AND** persistence or push is not required in V1

### Requirement: WebSocket heartbeat

The system MUST support Envelope heartbeat: `domain=system, type=ping` and `type=pong`; heartbeat MUST refresh Redis online TTL.

#### Scenario: Client ping

- **WHEN** a connected client sends `domain=system, type=ping`
- **THEN** the server replies with `domain=system, type=pong`
- **AND** refreshes the user Redis online key TTL

### Requirement: Persist before IM WebSocket ACK

When IM handles `message.send` over WebSocket, infra transport MUST NOT emit successful `message.ack` before IM persists the message in PostgreSQL.

#### Scenario: ACK ordering

- **WHEN** a user sends an IM message over WS
- **THEN** successful `type=message.ack` MUST NOT be sent before the message row exists in PostgreSQL

## MODIFIED Requirements

### Requirement: WebSocket infrastructure

The system SHALL expose a WebSocket endpoint at `/infra/ws` for realtime messaging.

#### Scenario: WebSocket handshake

- **WHEN** a client with a valid JWT connects to `/infra/ws`
- **THEN** the connection is established successfully
- **AND** the session is registered for message delivery
- **AND** the session binds JWT `tenant_id` and user id

#### Scenario: Envelope delivery

- **WHEN** a business module calls `RealtimeTransportApi` to push to a connected user
- **THEN** the client receives JSON conforming to the Envelope specification

### Requirement: WebSocket sender mode

The WebSocket layer SHALL support configurable `local` and `redis` sender modes.

#### Scenario: Single-instance mode

- **WHEN** `relayflow.websocket.sender-type=local`
- **THEN** push delivery completes within the same application instance

#### Scenario: Multi-instance mode

- **WHEN** `relayflow.websocket.sender-type=redis`
- **THEN** push is broadcast via Redis Pub/Sub channel `t:{tenantId}:ws:fanout` or equivalent
- **AND** the instance holding the target user session performs local delivery
