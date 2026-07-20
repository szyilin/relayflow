# Tasks：docs-schema-v1

> [平台] 子 change。范围：Flyway + Maven + codegen。无 `web/`、无业务 API。

- [x] 1. 根 `pom.xml` + BOM + `relayflow-module-docs`（api + biz）脚手架；`codegen.yml` 登记 `docs`
- [x] 2. Flyway：`doc_object`、`doc_library_node`（无 `doc_embed`）
- [x] 3. `./scripts/codegen.sh --module docs --tables doc_object,doc_library_node`（`--migrate` 如需）→ diff 合入 `*-biz/src/`
- [x] 4. `relayflow-server/pom.xml` 引入 `relayflow-module-docs-biz`
- [x] 5. `./mvnw -pl relayflow-server -am compile`
- [x] 6. `openspec validate docs-schema-v1 --strict`；更新母 change `workspace-docs-library-v1` tasks §1 勾选；看板注明 schema ready
