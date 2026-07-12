# 提案：IM 数据库 Schema v1（im-schema-v1）

## Why

[`im-platform-foundation`](../archive/2026-07-12-im-platform-foundation/design.md) 已定义统一会话模型与消息 seq/幂等规则，但尚无 `im_*` Flyway 表与 DO/Mapper。本 change 补齐 **数据层真源**，供 `im-direct-chat-*` 业务切片使用。

## What Changes

- 新增 `V0.1.0.6__init_im.sql`：`im_conversation`、`im_conversation_member`、`im_message`、`im_group`、`im_channel`
- CLI codegen 合并 DO/Mapper 至 `relayflow-module-im-biz/target/generated-sources/mybatis/`
- `im-biz` pom 引入 mybatis/tenant starter + build-helper

## Capabilities

### Modified Capabilities

- `im`：数据表结构落地（实现规格中的会话/消息模型）

## Impact

| 区域 | 影响 |
|------|------|
| `relayflow-server/.../db/migration/` | 新迁移脚本 |
| `relayflow-module-im-biz` | generated DO/Mapper + pom |
| `web/` | **不改** |

## 不在本 change

- Service / Controller / WebSocket IM 业务
- 通知中心表 `notify_inbox_item`
