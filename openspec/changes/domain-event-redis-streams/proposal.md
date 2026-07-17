## Why

system 与 im 通过 `ImBotApi` 形成启动期循环依赖，目前靠 `@Lazy` 掩盖。文档要求跨域副作用走领域消息；V1 用现有 Redis Streams，不上独立 MQ。

## What Changes

- framework：`DomainEventPublisher` + Redis Streams 传输 + Listener 注册
- system：邀请/激活后发领域事件，移除对 `ImBotApi` 的 `@Lazy` 注入
- im：订阅事件并调用本域 Bot 能力
- im 域内：`BotRuntime` 与 `ImMessageService` 环通过抽端口去掉 `@Lazy`（不必上 Streams）

## Capabilities

### New Capabilities

- `domain-event`: 跨域领域事件发布/消费契约（Redis Streams 传输）

### Modified Capabilities

- （无产品面 API 变更）

## Impact

- `relayflow-spring-boot-starter-redis`、`system-api/biz`、`im-biz`
- 依赖已有 Redis；无新中间件
- 回滚：恢复同步 `ImBotApi` 调用；无 Flyway（幂等可先内存/Redis SET）
