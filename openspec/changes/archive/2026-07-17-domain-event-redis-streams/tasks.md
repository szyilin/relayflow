## Status

**REVERTED（2026-07-17）**：实现已撤回，恢复 system↔im / BotRuntime↔ImMessage 的 `@Lazy` 同步调用。
原因：Redis Streams 实现缺少可靠 ack/重试/Outbox，失败易静默丢事件；V1 单体阶段暂以 `@Lazy` 止损。
Wave A（事务/TraceId）与 Wave C（分层边界）保留。本 change 暂不继续落地。

## 1. Framework messaging

- [ ] 1.1 Add DomainEvent envelope + DomainEventPublisher API in starter-redis
- [ ] 1.2 Implement Redis Streams publisher (after-commit) and listener container
- [ ] 1.3 Idempotency via Redis key on eventId

## 2. System events + remove Lazy ImBotApi

- [ ] 2.1 Add payload DTOs and event type constants in system-api
- [ ] 2.2 Replace ImBotApi calls with DomainEventPublisher in UserServiceImpl, AuthRegisterServiceImpl, MemberInviteServiceImpl, MemberInviteAcceptCardHandler
- [ ] 2.3 Remove @Lazy ImBotApi fields

## 3. IM listeners

- [ ] 3.1 Add listeners for `system.tenant_user.activated` and `system.member.invited`

## 4. BotRuntime cycle

- [ ] 4.1 Extract BotReplyService; BotRuntimeImpl depends on it instead of @Lazy ImMessageService

## 5. Verify

- [ ] 5.1 `./mvnw -pl relayflow-server -am compile`
