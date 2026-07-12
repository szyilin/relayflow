# 设计：IM 数据库 Schema v1（im-schema-v1）

## Context

- 父设计：[`im-platform-foundation`](../archive/2026-07-12-im-platform-foundation/design.md) §D5
- 当前 Flyway 最新序号：`V0.1.0.5`

## Goals / Non-Goals

**Goals:**

- 5 张 `im_*` 业务表，含 `tenant_id` 与公共字段
- 单聊去重：`direct_peer_low` / `direct_peer_high` + 部分唯一索引
- 消息幂等：`UNIQUE(tenant_id, client_msg_id)`（非空时）
- 会话内顺序：`(tenant_id, conversation_id, seq)` 索引
- codegen 生成 DO/Mapper

**Non-Goals:**

- 业务 API、枚举类手写（除 codegen `enum-columns` 后续补充）
- `notify_inbox_*` 表

## 表清单

| 表 | 说明 |
|----|------|
| `im_conversation` | 会话壳（direct/group/channel） |
| `im_conversation_member` | 成员、read_seq、unread_count |
| `im_message` | 消息体、seq、client_msg_id |
| `im_group` | 群元数据，1:1 conversation |
| `im_channel` | 频道元数据，1:1 conversation |

## 验证

```bash
./scripts/codegen.sh -m im -t im_conversation,im_conversation_member,im_message,im_group,im_channel --migrate
./mvnw -pl relayflow-module-im/relayflow-module-im-biz -am compile
openspec validate im-schema-v1 --strict
```
