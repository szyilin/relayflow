## ADDED Requirements

### Requirement: Admin LLM connection management

Authorized administrators SHALL be able to create, update, delete, list, and connectivity-test tenant LLM connections of type `openai_compatible`. API keys MUST be stored encrypted server-side and MUST NOT be returned in API responses; responses MAY expose `apiKeyConfigured` boolean only.

#### Scenario: Create connection without echoing secret

- **WHEN** an admin with `agent:llm:update` creates a connection with an API key
- **THEN** the key is persisted encrypted
- **AND** subsequent GET/list responses omit the raw key and set `apiKeyConfigured` true when a key exists

#### Scenario: Connectivity test requires test permission

- **WHEN** an admin without `agent:llm:test` calls the test endpoint
- **THEN** the request is denied by RBAC

#### Scenario: Update keeps previous key when blank

- **WHEN** an admin updates a connection and omits or leaves API key blank
- **THEN** the previously stored encrypted key is retained

### Requirement: Enabled models for workspace agents

Authenticated workspace members SHALL be able to list enabled LLM connections for the current tenant as model references (`code`, `name`, `modelName`) without secrets, for selecting an Agent `modelRef`.

#### Scenario: Member lists enabled models

- **WHEN** a logged-in member calls the enabled-model list API
- **THEN** only `enabled=true` connections of the current tenant are returned
- **AND** no API key material is included

### Requirement: Admin LLM settings UI

The admin console SHALL expose a model settings page under infrastructure navigation for managing LLM connections, gated by `agent:llm:query` (and update/test actions by their permissions).

#### Scenario: Nav entry for model settings

- **WHEN** an admin has `agent:llm:query`
- **THEN** the admin sidebar shows a model settings entry leading to the LLM connection management page
