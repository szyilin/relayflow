# 提案：IM 实时传输平台层（im-realtime-platform）

## Why

[`im-platform-foundation`](../archive/2026-07-12-im-platform-foundation/design.md) 已定义 WebSocket + 四层实时架构，但 `relayflow-spring-boot-starter-websocket` 与 `infra-biz` 传输层仍为空壳。须先落地 **L1 传输 + L2 事件入口**，后续 `im-schema-v1`、`im-direct-chat-*` 才能复用 `RealtimeTransportApi` 推送消息。

## What Changes

- 实现 `relayflow-spring-boot-starter-websocket`：JWT 握手、SessionRegistry、local/redis sender、Envelope 模型
- 在 `infra-api` 新增 `RealtimeTransportApi`、`RealtimeEventPublisher` 及 DTO/枚举
- 在 `infra-biz` 实现 WebSocket 端点、`DomainMessageRouter`、`PingPongHandler`、占位 notify/presence Handler、ApiImpl
- `infra-biz` 引入 `starter-websocket`、`starter-redis` 依赖
- `application.yml` 增加 `relayflow.websocket.*` 默认配置
- Security：WebSocket 握手路径鉴权（query token）

## Capabilities

### New Capabilities

（无新域）

### Modified Capabilities

- `infra`：落地 WebSocket Envelope、域路由、Transport/Publisher 契约、心跳与多实例 fanout 的 **平台实现**

## Impact

| 区域 | 影响 |
|------|------|
| `relayflow-framework/relayflow-spring-boot-starter-websocket` | 核心实现 |
| `relayflow-module-infra/relayflow-module-infra-api` | 新增 realtime 契约 |
| `relayflow-module-infra/relayflow-module-infra-biz` | WS 端点、Router、ApiImpl |
| `relayflow-server/application.yml` | `relayflow.websocket.*` |
| `relayflow-module-im` | **不改**（ImWebSocketHandler 在 `im-direct-chat-api`） |
| `web/` | **不改** |
| Flyway | **无** |

## 不在本 change

- `im_*` 表与 DO
- `ImWebSocketHandler`、发消息业务
- 通知中心、Presence 业务逻辑
- 前端 WS 客户端

## 前置

- 架构真源：[`im-platform-foundation` design](../archive/2026-07-12-im-platform-foundation/design.md) §D1–D9
- 规格真源：`openspec/specs/infra/spec.md`（WebSocket 相关需求）
- JWT + 租户：`relayflow-spring-boot-starter-security`、`starter-tenant` 已就绪

## 回滚

移除 `relayflow.websocket.enable=false` 可关闭 WS；回滚代码后无 DB 迁移影响。
