# 提案：IM 平台基础架构（im-platform-foundation）

## Why

RelayFlow 的 IM 模块（`relayflow-module-im`）与 WebSocket 基础设施（`starter-websocket`）目前仅有 Maven 骨架；`/app/messages` 前端为占位 UI。若在无统一架构真源的情况下直接实现单聊，极易把「系统通知、Bot 卡片、在线状态」等业务硬编码进 `im_message` 与 WS Handler，后期扩展成本极高。

本 change **不交付用户可见功能**，而是建立 IM 实时通讯的平台级设计真源：WebSocket 传输层、域事件抽象、数据模型约定、纵向切片路线图。后续 `im-*` 切片均引用本文档与规格增量。

## What Changes

- 新增 **四层实时架构** 设计真源：Transport（infra）→ RealtimeEvent（域事件）→ Delivery（投递编排）→ Product（IM 会话 / 通知中心）
- 确定 V1 传输方案：**原生 WebSocket + JSON envelope**（`domain` 字段区分 im / notify / presence / system）
- 定义 WS envelope 规范、会话 seq 幂等模型、Content Block 扩展结构
- 在 `*-api` 层 **预留接口**（`RealtimeEventPublisher`、`RealtimeTransportApi`、`NotifyInboxApi` 等），V1 仅实现 IM 相关子集
- 规格增量：扩展 `im` 与 `infra` 域需求（会话模型、消息 seq、平台 WS 行为）
- 明确 **后续纵向切片** 顺序（平台 → schema → 单聊 web/api/integrate → 群聊 → 频道）

## Capabilities

### New Capabilities

（无新域；能力归入已有 `im` 与 `infra` 规格增量）

### Modified Capabilities

- `im`：统一会话模型、消息 seq 与幂等、Content Block、发布者类型、跨模块事件发布接口（占位）
- `infra`：WebSocket envelope 与 domain 路由、租户会话绑定、Redis 多实例 fanout、RealtimeTransport 契约

## Impact

| 区域 | 本 change | 说明 |
|------|-----------|------|
| `openspec/changes/im-platform-foundation/` | ✅ 新建 | proposal / design / specs / tasks |
| `relayflow-framework/` | ❌ 不改 | 实现留给 `im-realtime-platform` |
| `relayflow-module-im/` | ❌ 不改 | 实现留给 `im-schema-v1` 及业务切片 |
| `relayflow-module-infra/` | ❌ 不改 | 实现留给 `im-realtime-platform` |
| `web/` | ❌ 不改 | 实现留给 `im-direct-chat-web` |
| Flyway / deploy | ❌ 不改 | 无迁移、无配置变更 |

## 不在本 change

- WebSocket starter 代码实现
- IM 表 Flyway 迁移与 DO 生成
- 单聊 / 群聊 / 频道业务 API 与 UI
- 通知中心表与 UI
- Bot、交互卡片、SSE 第二通道、Kafka

## 前置

- 脚手架：`relayflow-module-im`、`relayflow-spring-boot-starter-websocket` 模块已存在
- 规格基线：[`openspec/specs/im/spec.md`](../../specs/im/spec.md)、[`openspec/specs/infra/spec.md`](../../specs/infra/spec.md)
- 租户：`tenant-ready-foundation` 设计（WS 握手须绑定 `tenant_id`）

## 后续切片（引用本设计，各自独立 change）

| 顺序 | Change 名（建议） | 类型 |
|------|-------------------|------|
| 1 | `im-realtime-platform` | `[平台]` WS + RealtimeTransport |
| 2 | `im-schema-v1` | `[平台]` Flyway + codegen |
| 3 | `im-direct-chat-web` → `-api` → `-integrate` | 纵向切片 |
| 4 | `im-group-chat-*` | 纵向切片 |
| 5 | `im-channel-*` | 纵向切片 |
| 6 | `im-notify-inbox-*` | 纵向切片（V1.1+） |

## 回滚 / 迁移

本 change 仅文档与规格增量，**无运行时影响**。archive 后合并 spec delta 至 `openspec/specs/`；若需撤销，revert OpenSpec 文件即可。
