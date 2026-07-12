# 任务：IM 数据库 Schema v1（im-schema-v1）

> **Lane**：`[平台]` 后端 · 无 UI。

## 前置

- [x] 0.1 阅读 [`im-platform-foundation` design](../archive/2026-07-12-im-platform-foundation/design.md) §D5 与本 change `design.md`

## Flyway

- [x] 1.1 新增 `V0.1.0.6__init_im.sql`（5 张 `im_*` 表 + 索引）

## Codegen

- [x] 2.1 `./scripts/codegen.sh -m im -t im_conversation,im_conversation_member,im_message,im_group,im_channel --migrate`
- [x] 2.2 合并 DO/Mapper 至 `relayflow-module-im-biz/target/generated-sources/mybatis/`

## im-biz 基座

- [x] 3.1 `relayflow-module-im-biz/pom.xml`：mybatis/tenant/common + lombok + build-helper
- [x] 3.2 `./mvnw -pl relayflow-module-im/relayflow-module-im-biz -am compile`

## 归档

- [x] 4.1 `openspec validate im-schema-v1 --strict`

## 不在本 change

- ImMessageService / Controller / `web/`
