## Context

承接 `workspace-docs-drive-v1` D1。前置 `doc_object` / `doc_library_node` 已在 `V0.1.0.34`。

## Goals / Non-Goals

**Goals:** Flyway 可迁移；codegen DO/Mapper 合入；compile。

**Non-Goals:** Controller、Service、infra FileApi 接线、前端。

## Decisions

### D1：表

同母 design：`doc_drive_folder`、`doc_drive_item`（`object_id` UNIQUE deleted=0）；`doc_object.type` ∈ `{RICH_DOC, FILE}`；`storage_file_id BIGINT NULL`。

### D2：FILE 与 body

`body` 保持 NOT NULL + 默认空 TipTap JSON；FILE 行忽略 body，以 `storage_file_id` 为准。CHECK：`type='FILE' ⇒ storage_file_id IS NOT NULL`；`type='RICH_DOC' ⇒ storage_file_id IS NULL`。

### D3：无头表

无 `doc_drive` 容器头表；根 = `parent_id`/`folder_id` NULL。

## Risks

- 改 CHECK 须 DROP/ADD；已有数据仅 RICH_DOC，安全。
