## ADDED Requirements

### Requirement: IM conversation tables

The system MUST provide Flyway DDL for `im_conversation`, `im_conversation_member`, `im_message`, `im_group`, and `im_channel` with `im_` prefix and `tenant_id` on all business tables.

#### Scenario: Migration applies on startup

- **WHEN** Flyway runs on a fresh database after prior migrations
- **THEN** all five `im_*` tables exist with primary keys and tenant-scoped indexes

#### Scenario: Direct conversation deduplication columns

- **WHEN** `im_conversation.type` is `direct`
- **THEN** the table provides `direct_peer_low` and `direct_peer_high` for tenant-scoped pair uniqueness

#### Scenario: Message idempotency index

- **WHEN** `im_message.client_msg_id` is not null
- **THEN** duplicate `(tenant_id, client_msg_id)` inserts are prevented by a unique index
