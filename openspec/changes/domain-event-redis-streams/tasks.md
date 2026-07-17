## 1. Framework messaging

- [x] 1.1 Add DomainEvent envelope + DomainEventPublisher API in starter-redis
- [x] 1.2 Implement Redis Streams publisher (after-commit) and listener container
- [x] 1.3 Idempotency via Redis key on eventId

## 2. System events + remove Lazy ImBotApi

- [x] 2.1 Add payload DTOs and event type constants in system-api
- [x] 2.2 Replace ImBotApi calls with DomainEventPublisher in UserServiceImpl, AuthRegisterServiceImpl, MemberInviteServiceImpl, MemberInviteAcceptCardHandler
- [x] 2.3 Remove @Lazy ImBotApi fields

## 3. IM listeners

- [x] 3.1 Add listeners for `system.tenant_user.activated` and `system.member.invited`

## 4. BotRuntime cycle

- [x] 4.1 Extract BotReplyService; BotRuntimeImpl depends on it instead of @Lazy ImMessageService

## 5. Verify

- [x] 5.1 `./mvnw -pl relayflow-server -am compile`
