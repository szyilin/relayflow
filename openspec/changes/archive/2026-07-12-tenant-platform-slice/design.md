# 设计：租户平台层收尾

## 验收清单（§5）

| 任务 | 真源 | 验收方式 |
|------|------|----------|
| 5.1 infra DO | codegen → `TenantBaseDO` | 编译 + 表 `tenant_id` |
| 5.2 im DO | 同上 | 编译 + `V0.1.0.6__init_im.sql` |
| 5.3 Redis | `TenantRedisKeyBuilder` | 权限缓存、token 吊销、WS online/fanout |
| 5.4 MinIO | objectKey 前缀 | 已在 `infra-file-upload-api` 完成 |
| 5.5 WS | `JwtWebSocketHandshakeInterceptor` + `WebSocketSessionRegistry(UserKey)` | 单元测试 + fanout null 校验 |

## 默认租户保护

```java
TenantService.assertDeletable(tenantId)
  → tenantId == defaultId → TENANT_DEFAULT_DELETE_FORBIDDEN
```

V1 无删除租户 API；方法供将来扩展与测试断言。

## 测试

```bash
./mvnw -pl relayflow-framework/relayflow-spring-boot-starter-tenant,relayflow-framework/relayflow-spring-boot-starter-redis,relayflow-framework/relayflow-spring-boot-starter-websocket,relayflow-module-system/relayflow-module-system-biz test
./mvnw -pl relayflow-server -am compile
```
