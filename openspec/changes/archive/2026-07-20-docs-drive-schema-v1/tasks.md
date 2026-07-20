# Tasks：docs-drive-schema-v1

## 1. Flyway

- [x] 1.1 新增迁移：`doc_drive_folder`、`doc_drive_item`；`doc_object` 增加 `storage_file_id`；放宽 type CHECK 含 `FILE`；FILE/RICH_DOC 与 `storage_file_id` 互斥 CHECK；索引按 design
- [x] 1.2 （可选）`codegen.yml` 已含 `doc_*` 则无需改；否则补表清单

## 2. Codegen 合入

- [x] 2.1 `./scripts/codegen.sh --module docs --tables doc_object,doc_drive_folder,doc_drive_item --migrate`（或等价）→ 临时目录 → diff 合入 `*-biz/src/`
- [x] 2.2 `./mvnw -pl relayflow-server -am compile`

## 3. 收尾

- [x] 3.1 `openspec validate docs-drive-schema-v1 --strict`
- [x] 3.2 勾选母 change `workspace-docs-drive-v1` §1；看板注明 schema 就绪
