# 平台契约：tenant-platform-slice

## Redis key

格式：`t:{tenantId}:{namespace}:{suffix}`

示例：

- 权限缓存：`t:1:auth:perms:100`
- WS fanout channel：`t:1:ws:fanout`

## WebSocket

- 握手 JWT 须含 `tenant_id` claim
- `WebSocketSessionRegistry` 按 `(tenantId, userId)` 索引

## 验证命令

```bash
./mvnw -pl relayflow-framework/relayflow-spring-boot-starter-tenant,relayflow-framework/relayflow-spring-boot-starter-redis,relayflow-framework/relayflow-spring-boot-starter-websocket,relayflow-module-system/relayflow-module-system-biz test
```
