## ADDED Requirements

### Requirement: Domain event publish and consume

The system SHALL provide a framework `DomainEventPublisher` that publishes domain events to Redis Streams, and SHALL allow modules to register typed listeners that consume those events asynchronously.

#### Scenario: Publish after local commit

- **WHEN** business code publishes a domain event while a Spring transaction is active
- **THEN** the event is written to Redis Streams only after the transaction commits successfully

#### Scenario: Listener handles event

- **WHEN** an event of a registered `eventType` is available on the stream
- **THEN** the matching listener is invoked with the envelope and deserialized payload
- **AND** duplicate delivery of the same `eventId` does not re-execute side effects (idempotent)

### Requirement: Break system-im Lazy coupling via events

System membership activation and member-invite notifications SHALL be published as domain events; IM SHALL consume them. System biz SHALL NOT inject `ImBotApi` for these side effects.

#### Scenario: Member activated

- **WHEN** a user becomes an active tenant member (register / accept invite / admin create)
- **THEN** system publishes `system.tenant_user.activated` with tenantId and userId
- **AND** IM ensures bot enablements for that user

#### Scenario: Member invited

- **WHEN** an admin invites a member and invite bot notify is required
- **THEN** system publishes `system.member.invited`
- **AND** IM sends the org-assistant invite card (best-effort)
