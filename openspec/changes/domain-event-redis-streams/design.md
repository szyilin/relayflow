## Context

Break system↔im `@Lazy` with DomainEvent + Redis Streams; break BotRuntime↔ImMessageService with a narrow writer port.

## Decisions

1. Publisher lives in `relayflow-spring-boot-starter-redis` under `com.relayflow.framework.messaging`.
2. Stream key: `de:{eventType}` (global; tenantId in envelope payload).
3. Consumer group: `cg:{listenerBeanName}` or fixed `cg:relayflow`.
4. Publish after transaction commit via `TransactionSynchronizationManager` when a TX is active; otherwise immediate XADD.
5. Idempotency: Redis SET `de:idemp:{eventId}` with TTL 7d before handler runs.
6. Events in system-api:
   - `system.tenant_user.activated` → ensureUserEnablementsOnActive
   - `system.member.invited` → invite bot card send
7. BotRuntime: introduce `BotReplySender` implemented by a thin class that only depends on message persistence helpers, OR inject a dedicated `ImBotReplyService` that ImMessageServiceImpl also uses — simplest: extract interface `BotMessageSender` with `sendBotReply`, implement in new class that takes mappers, leave ImMessageServiceImpl calling same impl — actually simplest break: create `ImBotReplyService` / `BotReplyService` bean that `BotRuntimeImpl` and optionally message service use.

Simplest BotRuntime fix: move `sendBotReply` to `ImBotReplyService` (new), both `ImMessageServiceImpl` (if needed) and `BotRuntimeImpl` depend on it; `ImMessageServiceImpl` no longer injects anything that leads to BotRuntime at construct... wait, the cycle is:

ImMessageServiceImpl → GroupBotMentionDispatcher → BotIngress → BotRuntime → ImMessageService

So BotRuntime should depend on a new `BotReplyService` that does what `sendBotReply` does, without going through ImMessageServiceImpl. Extract method body to BotReplyServiceImpl.

## Non-Goals

- Outbox table
- Independent MQ
- Changing invite product behavior
