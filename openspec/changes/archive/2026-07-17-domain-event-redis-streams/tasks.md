## Status

**REVERTED（2026-07-17）**：实现已撤回，恢复 system↔im / BotRuntime↔ImMessage 的 `@Lazy` 同步调用。
原因：Redis Streams 实现缺少可靠 ack/重试/Outbox，失败易静默丢事件；V1 单体阶段暂以 `@Lazy` 止损。
Wave A（事务/TraceId）与 Wave C（分层边界）保留。本 change 暂不继续落地。

## 1. Framework messaging

- [x] 1.1 Add DomainEvent envelope + DomainEventPublisher API in starter-redis — **关闭（路线重置，不再作为当前 backlog）**
- [x] 1.2 Implement Redis Streams publisher (after-commit) and listener container — **关闭（路线重置，不再作为当前 backlog）**
- [x] 1.3 Idempotency via Redis key on eventId — **关闭（路线重置，不再作为当前 backlog）**

## 2. System events + remove Lazy ImBotApi

- [x] 2.1 Add payload DTOs and event type constants in system-api — **关闭（路线重置，不再作为当前 backlog）**
- [x] 2.2 Replace ImBotApi calls with DomainEventPublisher in UserServiceImpl, AuthRegisterServiceImpl, MemberInviteServiceImpl, MemberInviteAcceptCardHandler — **关闭（路线重置，不再作为当前 backlog）**
- [x] 2.3 Remove @Lazy ImBotApi fields — **关闭（路线重置，不再作为当前 backlog）**

## 3. IM listeners

- [x] 3.1 Add listeners for `system.tenant_user.activated` and `system.member.invited` — **关闭（路线重置，不再作为当前 backlog）**

## 4. BotRuntime cycle

- [x] 4.1 Extract BotReplyService; BotRuntimeImpl depends on it instead of @Lazy ImMessageService — **关闭（路线重置，不再作为当前 backlog）**

## 5. Verify

- [x] 5.1 `./mvnw -pl relayflow-server -am compile` — **关闭（路线重置，不再作为当前 backlog）**
