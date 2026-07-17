# service-boundary Specification

## Purpose

跨模块 API 与分层边界约定：`*ApiImpl` 不得依赖 Controller VO。

## Requirements

### Requirement: Cross-module APIs must not depend on Controller VO

Module `*ApiImpl` classes SHALL map from domain/internal models or DO to `*-api` DTOs, and MUST NOT import `controller.*.vo` types.

#### Scenario: Task search API

- **WHEN** `TaskItemApi.searchTasks` is invoked
- **THEN** the implementation does not reference `TaskItemRespVO`

#### Scenario: Conversation search API

- **WHEN** `ImConversationApi.searchConversations` is invoked
- **THEN** the implementation does not reference `ConversationItemRespVO`
