# 任务：IM 实时传输平台层（im-realtime-platform）

> **Lane**：`[平台]` 后端 · 无 UI。  
> **前置**：阅读 [`im-platform-foundation` design](../archive/2026-07-12-im-platform-foundation/design.md) 与本 change `design.md`。

## 1. 前置

- [ ] 1.1 阅读本 change `proposal.md`、`design.md` 与 `openspec/specs/infra/spec.md` WebSocket 相关需求

## 2. Framework（relayflow-spring-boot-starter-websocket）

- [ ] 2.1 `WebSocketProperties` + `RealtimeEnvelope` + `WebSocketSessionRegistry` + `WebSocketSessionInfo`
- [ ] 2.2 `JwtWebSocketHandshakeInterceptor`（query `token` → userId + tenantId）
- [ ] 2.3 `LocalWebSocketMessageSender` + `RedisWebSocketMessageSender` + `WebSocketAutoConfiguration`
- [ ] 2.4 `starter-websocket` 依赖 security、redis；注册 `META-INF/spring/...AutoConfiguration.imports`

## 3. Infra API + Biz

- [ ] 3.1 `infra-api`：`RealtimeTransportApi`、`RealtimeEventPublisher`、`RealtimeDomainMessageHandler`、DTO/枚举
- [ ] 3.2 `infra-biz`：`InfraWebSocketHandler`、`DomainMessageRouter`、`SystemPingPongHandler`、NoOp notify/presence
- [ ] 3.3 `RealtimeTransportApiImpl`、`RealtimeEventPublisherImpl`；`infra-biz` 引入 `starter-websocket`、`starter-redis`
- [ ] 3.4 Security：`/infra/ws` HTTP upgrade `permitAll`；`application.yml` 增加 `relayflow.websocket.*`

## 4. 验证与归档

- [ ] 4.1 `./mvnw -pl relayflow-server -am compile`
- [ ] 4.2 `openspec validate im-realtime-platform --strict`
- [ ] 4.3 手工或集成测试：WS 连接 + `ping`→`pong` + `RealtimeTransportApi` 下行推送

## 不在本 change

- `im-biz` Handler、`im_*` 表、`web/` WS 客户端

## 下一 change

- `im-schema-v1`（Flyway）或与 `im-direct-chat-web` 并行启动
