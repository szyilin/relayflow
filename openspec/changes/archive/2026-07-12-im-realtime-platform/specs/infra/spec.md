## ADDED Requirements

### Requirement: WebSocket starter session registry

The framework WebSocket starter SHALL provide a tenant-scoped `WebSocketSessionRegistry` that maps `(tenantId, userId)` to active sessions and supports multi-session per user.

#### Scenario: Register on connect

- **WHEN** a WebSocket handshake succeeds with valid JWT
- **THEN** the session is registered with `tenantId` and `userId` from token claims

#### Scenario: Unregister on disconnect

- **WHEN** the last session for a `(tenantId, userId)` pair closes
- **THEN** the registry removes the mapping
- **AND** the Redis online key for that user MAY be deleted

### Requirement: WebSocket domain handler SPI

The infra module SHALL expose `RealtimeDomainMessageHandler` SPI and a `DomainMessageRouter` that dispatches upstream Envelopes by `domain`; additional handlers MUST be registrable via Spring without modifying infra core classes.

#### Scenario: System handler registered

- **WHEN** an upstream envelope has `domain=system, type=ping`
- **THEN** the system ping/pong handler processes the message

#### Scenario: Placeholder notify handler

- **WHEN** an upstream envelope has `domain=notify`
- **THEN** the placeholder handler completes without error
- **AND** the WebSocket connection remains open

### Requirement: WebSocket platform configuration

The application SHALL bind `relayflow.websocket.*` properties including `enable`, `path`, `sender-type`, and `heartbeat-ttl-seconds`.

#### Scenario: Disabled websocket

- **WHEN** `relayflow.websocket.enable=false`
- **THEN** the WebSocket endpoint is not registered

#### Scenario: Default path

- **WHEN** `relayflow.websocket.enable=true` and path is not overridden
- **THEN** the endpoint is available at `/infra/ws`
