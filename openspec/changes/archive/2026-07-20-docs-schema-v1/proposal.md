# 提案：docs-schema-v1（[平台]）

## Why

母 change [`workspace-docs-library-v1`](../workspace-docs-library-v1/proposal.md) 需要 `doc_` 域表与 Maven 模块，才能继续 `-web` / `-api`。本 change 只做平台脚手架，无业务 API、无 `web/`。

## What Changes

1. 新建 `relayflow-module-docs`（`*-api` + `*-biz`），根 reactor 与 BOM、`relayflow-server` 加载 `docs-biz`
2. Flyway：`doc_object`、`doc_library_node`（含 `body` JSONB、`body_format`；**无** `doc_embed`）
3. codegen 合入 DO / Mapper / Mapper.xml；`codegen.yml` 登记 `docs` module
4. `./mvnw -pl relayflow-server -am compile` 通过

## Capabilities

### New Capabilities

- （无独立新 capability 名；行为并入母 change 的 `docs`，本切片以 schema/模块就绪为准）

### Modified Capabilities

- `docs`（母 change delta）：确认 Library V1 两表就绪且无 `doc_embed`

## Impact

| 层 | 变更 |
|----|------|
| Maven | 新模块 + BOM + server 依赖 |
| DB | `V0.1.0.34__docs_schema.sql`（序号以仓库最新为准） |
| 工具 | `codegen.yml` 增加 `docs` |
| 前端 | 无 |

## 非目标

- Controller / Service / App API
- TipTap、导出、分享
- `doc_embed`、云盘、知识库表
