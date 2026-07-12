# Tasks：tenant-platform-slice

> **前置**：[`tenant-ready-foundation`](../tenant-ready-foundation/tasks.md) §2–4、§6 已完成

## 1. 平台验收（§5）

- [x] 1.1 确认 infra/im DO 继承 `TenantBaseDO`（codegen 产物）
- [x] 1.2 WS fanout 订阅校验 `tenantId` 非空
- [x] 1.3 `TenantService.assertDeletable` + 错误码

## 2. 测试（§7）

- [x] 2.1 `RelayflowTenantLineHandler` 单租户/上下文切换
- [x] 2.2 `TenantRedisKeyBuilder` 前缀格式
- [x] 2.3 `WebSocketSessionRegistry` 跨租户 session 隔离
- [x] 2.4 默认租户不可删除断言

## 3. 归档 tenant-ready-foundation

- [x] 3.1 同步 `openspec/specs/deployment`、`system` delta
- [x] 3.2 更新 `AGENTS.md`「下一优先」
- [x] 3.3 `openspec validate tenant-platform-slice --strict`
- [x] 3.4 archive `tenant-ready-foundation` + `tenant-platform-slice`

## 4. 验证

- [x] 4.1 `./mvnw -pl relayflow-framework/relayflow-spring-boot-starter-tenant,relayflow-framework/relayflow-spring-boot-starter-redis,relayflow-framework/relayflow-spring-boot-starter-websocket,relayflow-module-system/relayflow-module-system-biz -am test`
- [x] 4.2 `./mvnw -pl relayflow-server -am compile`
