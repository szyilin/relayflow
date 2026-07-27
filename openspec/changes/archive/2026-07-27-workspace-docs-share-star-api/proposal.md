## Why

`-web` 已 ui_ready；按 `openspec/lanes/workspace-docs-share-star/contract.md` 实现后端。

## What Changes

- Flyway：`doc_share`、`doc_star`、`doc_user_visit` + `docs-bot` 种子
- ACL：`DocAccessService` 支持 owner ∪ share
- REST：share / shared / star / recent（visit）按 contract
- best-effort `ImBotApi.send(docs-bot)`
- 文档 GET/PUT/export 走 ACL；打开 upsert visit

## Capabilities

### New Capabilities

- （无独立 capability；行为见母 change）

### Modified Capabilities

- （本 lane 不写主 specs delta；母 change 已有）

## Impact

- `relayflow-module-docs-*`、Flyway、`docs-biz` → `im-api`
- 看板 api 状态
