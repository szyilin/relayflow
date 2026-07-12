# 设计：IM 实时传输平台层（im-realtime-platform）

## Context

- 父设计：[`im-platform-foundation`](../archive/2026-07-12-im-platform-foundation/design.md)
- 现状：`starter-websocket` 仅 `package-info.java`；`infra-api` 无 realtime 契约；`infra-biz` 无 WS 端点
- 目标：实现 **L1 Transport + L2 Publisher 入口**；IM 业务 Handler 由后续 `im-direct-chat-api` 注册

## Goals / Non-Goals

**Goals:**

- `/infra/ws` 可连接、JWT 鉴权、tenant/user 绑定
- Envelope 上下行解析与 `domain` 路由
- `RealtimeTransportApi` 单用户/多用户推送
- `local` / `redis` sender 可配置切换
- `ping`/`pong` + Redis 在线 TTL
- `RealtimeEventPublisher` + notify/presence **NoOp** 占位
- `DomainMessageHandler` SPI，供 im-biz 后续注册

**Non-Goals:**

- IM 消息持久化与 `message.send` 业务处理
- `im-biz` 代码变更
- Flyway、前端
- STOMP 协议

---

## Decisions

### D1：包结构

#### starter-websocket（framework）

```text
com.relayflow.framework.websocket
  ├── config/
  │     WebSocketProperties          # relayflow.websocket.*
  │     WebSocketAutoConfiguration
  ├── core/
  │     RealtimeEnvelope             # domain, type, requestId, ts, payload
  │     WebSocketSessionInfo         # tenantId, userId, sessionId
  │     WebSocketSessionRegistry     # (tenantId,userId) -> sessions
  │     WebSocketMessageSender       # 接口
  ├── sender/
  │     LocalWebSocketMessageSender
  │     RedisWebSocketMessageSender  # Pub/Sub fanout
  └── security/
        JwtWebSocketHandshakeInterceptor
```

`starter-websocket` 依赖：`starter-security`（JwtTokenService）、`starter-redis`（redis 模式）、`spring-boot-starter-websocket`。

#### infra-api

```text
com.relayflow.module.infra.api.realtime
  ├── RealtimeTransportApi
  ├── RealtimeEventPublisher
  ├── RealtimeDomainMessageHandler   # SPI：im 模块后续实现
  ├── dto/RealtimeEnvelopeDTO, RealtimeEventDTO
  └── enums/RealtimeDomain, RealtimeSystemType
```

#### infra-biz

```text
com.relayflow.module.infra
  ├── api/realtime/
  │     RealtimeTransportApiImpl
  │     RealtimeEventPublisherImpl
  ├── websocket/
  │     InfraWebSocketHandler          # 统一 Text 入口
  │     DomainMessageRouter
  │     handler/
  │           SystemPingPongHandler    # domain=system
  │           NoOpNotifyHandler
  │           NoOpPresenceHandler
  └── framework/realtime/
        RealtimeWebSocketConfiguration # 注册 Endpoint + Router beans
```

### D2：WebSocketProperties

```yaml
relayflow:
  websocket:
    enable: true
    path: /infra/ws
    sender-type: local          # local | redis
    allowed-origins: "*"        # V1 开发宽松；生产由部署收紧
    heartbeat-ttl-seconds: 60   # Redis online key TTL
    token-query-param: token    # 握手 ?token=
```

### D3：握手与鉴权

1. 客户端：`ws://host/infra/ws?token=<accessToken>`
2. `JwtWebSocketHandshakeInterceptor`：
   - 从 query 读取 token（V1 不用 Sec-WebSocket-Protocol）
   - `JwtTokenService.parseClaims` → `userId`、`tenant_id`
   - 失败 → 握手拒绝（HTTP 401）
3. `afterConnectionEstablished`：注册 `WebSocketSessionRegistry`，写 Redis `t:{tenantId}:online:{userId}`

**Security HTTP 配置**：`/infra/ws` 对 HTTP upgrade 请求 `permitAll`（鉴权在 HandshakeInterceptor 完成）。

### D4：Envelope 与路由

```java
@Data
public class RealtimeEnvelope {
    private String domain;      // im | notify | presence | system
    private String type;
    private String requestId;
    private Long ts;
    private Object payload;
}
```

`InfraWebSocketHandler`：
- 收 Text → JSON parse → `DomainMessageRouter.route(envelope, sessionInfo)`
- 未知 domain → 忽略 + debug log

`RealtimeDomainMessageHandler` SPI：

```java
public interface RealtimeDomainMessageHandler {
    String domain();  // 如 "im"
    void onMessage(RealtimeEnvelope envelope, WebSocketSessionInfo session);
}
```

V1 注册：`system`（PingPong）、`notify`（NoOp）、`presence`（NoOp）。**`im` Handler 由 `im-direct-chat-api` 通过 Spring 注入注册，本 change 不实现。**

### D5：Ping/Pong

`SystemPingPongHandler`（`domain=system`）：
- `type=ping` → 回复 `pong` envelope；刷新 Redis online TTL
- 其他 system type → 忽略

### D6：RealtimeTransportApiImpl

```java
public void sendToUser(Long tenantId, Long userId, RealtimeEnvelopeDTO envelope) {
    messageSender.send(tenantId, List.of(userId), toFrameworkEnvelope(envelope));
}
```

`LocalWebSocketMessageSender`：查 `WebSocketSessionRegistry` → `session.sendMessage(TextMessage(json))`

`RedisWebSocketMessageSender`：
- publish 至 `t:{tenantId}:ws:fanout`
- payload: `{ tenantId, userIds, envelope, sourceInstanceId }`
- 各实例 `@RedisListener` → 本地 Registry 投递

### D7：RealtimeEventPublisherImpl

```java
public void publish(RealtimeEventDTO event) {
    switch (event.domain()) {
        case NOTIFY, PRESENCE -> log.debug("no-op {}", event);
        case IM -> delegate to registered RealtimeDomainMessageHandler or TransportApi per event type
        case SYSTEM -> optional transport push
    }
}
```

V1：`notify`/`presence` no-op；`im` 事件若无 handler 则 debug log（待 im 模块注册）。

### D8：在线状态

- Key：`t:{tenantId}:online:{userId}`
- 值：`instanceId` 或 `1`
- TTL：`heartbeat-ttl-seconds`；ping 与连接建立时续期
- 断开连接：删除 key（若该用户无其他 session）

多设备：Registry 支持同一 user 多 session；全部断开才删 online key。

### D9：依赖变更

**infra-biz/pom.xml** 增加：

```xml
<dependency>
  <groupId>com.relayflow</groupId>
  <artifactId>relayflow-spring-boot-starter-websocket</artifactId>
</dependency>
<dependency>
  <groupId>com.relayflow</groupId>
  <artifactId>relayflow-spring-boot-starter-redis</artifactId>
</dependency>
```

**starter-websocket/pom.xml** 增加 security、redis、jackson 依赖。

### D10：Domain Handler 注册机制

```java
@Configuration
public class RealtimeWebSocketConfiguration {
    @Bean
    public DomainMessageRouter domainMessageRouter(List<RealtimeDomainMessageHandler> handlers) {
        return new DomainMessageRouter(handlers);
    }
}
```

后续 `im-biz` 提供 `@Component` 实现 `RealtimeDomainMessageHandler`（`domain()="im"`）即可自动注册。

---

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| Query token 泄露 Referer | 文档建议短期 token；生产 HTTPS |
| Redis 不可用且 sender-type=redis | 启动校验或降级 local（V1 启动失败明确报错） |
| 无 im handler 时 domain=im 上行无响应 | 本 change 验收仅 ping/pong + Transport 下行；im 在下一切片 |
| CORS/WebSocket origin | `allowed-origins` 可配置 |

---

## 验证

```bash
./mvnw -pl relayflow-server -am compile
openspec validate im-realtime-platform --strict
```

**手工冒烟**（实现后）：

1. 登录获取 JWT
2. `wscat -c "ws://localhost:8080/infra/ws?token=..."` 
3. 发送 `{"domain":"system","type":"ping","requestId":"1","ts":0,"payload":{}}` → 收到 `pong`
4. 单元/集成测试：`RealtimeTransportApi.sendToUser` 推送至已连接 session

---

## 后续衔接

- `im-direct-chat-api`：实现 `ImDomainMessageHandler`，处理 `message.send` 等
- `im-schema-v1`：与 WS 平台并行或紧随其后，无硬依赖
