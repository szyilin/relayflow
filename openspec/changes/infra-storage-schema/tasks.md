# 任务：infra-storage-schema

> **Lane**：`[平台]` 后端 · 无 UI。  
> **前置**：① `infra-storage-platform` 已实施；阅读本 change `design.md` 与父史诗 §D2–D4、D7。

## 前置

- [x] 0.1 阅读 `design.md` 与 [`infra-storage-v1/design.md`](../infra-storage-v1/design.md) 表结构章节

## Flyway

- [x] 1.1 新增 `V0.1.0.5__init_infra_storage.sql`：`infra_storage_provider`、`infra_file`、`infra_file_upload_session`、`infra_file_binding` DDL
- [x] 1.2 同脚本种子 `infra:storage:*`、`infra:file:*` 权限并绑定 `super_admin`

## Codegen

- [x] 2.1 `./scripts/codegen.sh -m infra -t infra_storage_provider,infra_file,infra_file_upload_session,infra_file_binding --migrate`
- [x] 2.2 diff 后合并 DO/Mapper 至 `relayflow-module-infra-biz/target/generated-sources/mybatis/`

## infra-biz 基座

- [x] 3.1 `relayflow-module-infra-biz/pom.xml` 引入 web/security/mybatis/tenant/oss starters + lombok + build-helper
- [x] 3.2 `./mvnw -pl relayflow-module-infra/relayflow-module-infra-biz -am compile`

## 归档

- [x] 4.1 `openspec validate infra-storage-schema --strict`
- [x] 4.2 父史诗 `infra-storage-v1/tasks.md` 记录 ② 立项（本 change 完成后）

## 不在本 change

- StorageProviderService / FileService / Controller
- `web/` 菜单与页面
