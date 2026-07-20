# 设计：docs-schema-v1

## Context

对齐母 change design D1/D4：两表、`body_format`、无 embed。脚手架对标 `relayflow-module-calendar`。

## Goals / Non-Goals

**Goals:** 模块可被 server 加载；表可迁移；DO/Mapper 进 Git；compile 通过。

**Non-Goals:** 任何业务接口与 UI。

## Decisions

### D1：表结构

见 Flyway；要点：

- `doc_object.type` CHECK `RICH_DOC`（V1）
- `body` JSONB NOT NULL，默认空 TipTap doc：`{"type":"doc","content":[]}`
- `body_format` 默认 `tiptap_json_v1`
- `content_version` 默认 `0`
- `doc_library_node.object_id` UNIQUE（deleted=0）
- 索引：`(tenant_id, owner_user_id)` on both；node `(tenant_id, owner_user_id, parent_id)`

### D2：biz 依赖

`docs-api` + `system-api` + web/security/mybatis/tenant starters（与 calendar 同级精简；**不**引 im-api / infra-api）。

### D3：codegen

`--module docs --tables doc_object,doc_library_node`（需先 `--migrate` 或库已有表）。

## Risks

本地无 Postgres 时 codegen `--migrate` 失败 → 先 `docker compose up -d` 或手写对齐 calendar 风格的三件套后再 compile（仍应用 CLI 生成 diff 校验）。

## Open Questions

无（母 change 已拍板）。
