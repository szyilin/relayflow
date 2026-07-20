## Why

母 change [`workspace-docs-drive-v1`](../workspace-docs-drive-v1/proposal.md) 需要云盘文件夹树与 FILE 对象落库。本 change 仅做 **[平台] 表结构 + DO/Mapper**，不写业务 API/UI。

## What Changes

- Flyway：新建 `doc_drive_folder`、`doc_drive_item`；`doc_object` 扩展 `FILE` + `storage_file_id`
- codegen 合入 DO/Mapper；`relayflow-server` compile 通过

## Capabilities

### Modified Capabilities

- `docs`: 云盘放置表与 FILE 类型列（行为 API 在后续 `-api`）

## Impact

- 迁移：`V0.1.0.35__docs_drive_schema.sql`（序号以仓库最新为准）
- 模块：`relayflow-module-docs-biz` 数据层
- 依赖：不新增 Maven 依赖（`FileApi` 在 `-api` 切片引入）
