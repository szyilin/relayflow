# 设计：存储域表结构 — infra-storage-schema

## Context

- 父史诗：[`infra-storage-v1`](../infra-storage-v1/design.md) §D2、D3、D4、D7
- 前置：① `infra-storage-platform`（`ObjectStorageClient` 已就绪）
- 现状：`infra-biz` 仅依赖 `infra-api`；无 `infra_*` Flyway；无 generated DO/Mapper

## Goals / Non-Goals

**Goals:**

- 四张 `infra_*` 表符合 [`database.md`](../../../docs/dev/database.md) 公共字段约定
- 权限种子与史诗 D7 一致，绑定 `super_admin`（role id=100）
- CLI codegen 生成 DO/Mapper 至 `target/generated-sources/mybatis/`
- `infra-biz` pom 对齐 `system-biz` starters 模式（含 `starter-oss`）

**Non-Goals:**

- Service / Controller / 密钥加密
- 菜单种子（`-web` 切片再补）
- `web/` 改动

## Decisions

### D1：迁移文件拆分

单文件 `V0.1.0.5__init_infra_storage.sql` 含 DDL + 权限种子（同 `V0.1.0.2` 模式），避免同版本多文件碎片化。

### D2：表结构

**`infra_storage_provider`**（租户多 provider 配置）

| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGINT PK | 雪花 |
| tenant_id | BIGINT | 租户 |
| provider | VARCHAR(32) | `minio` 等 |
| status | VARCHAR(16) | `active` / `legacy` |
| is_default | SMALLINT | 0/1，租户内至多一个默认 |
| config_json | TEXT | endpoint、bucket、access_key、secret_key_enc、use_ssl、path_prefix |
| creator/updater/create_time/update_time/deleted | 标准 | |

索引：`idx_infra_storage_provider_tenant`；`uk_infra_storage_provider_tenant_provider`（deleted=0）

**`infra_file`**（文件元数据）

| 列 | 类型 | 说明 |
|----|------|------|
| provider | VARCHAR(32) | 写入时 provider |
| storage_uri | VARCHAR(512) | 如 `minio://bucket/key` |
| object_key | VARCHAR(512) | `tenant/{id}/files/...` |
| original_name | VARCHAR(256) | 原始文件名 |
| mime_type | VARCHAR(128) | |
| size | BIGINT | 字节 |
| sha256 | VARCHAR(64) | 可选 |
| access_level | VARCHAR(16) | `public` / `private` |

索引：`idx_infra_file_tenant_provider`；`idx_infra_file_tenant_create`

**`infra_file_upload_session`**（直传会话）

| 列 | 类型 | 说明 |
|----|------|------|
| status | VARCHAR(16) | `pending` / `confirmed` / `expired` |
| provider | VARCHAR(32) | |
| object_key | VARCHAR(512) | |
| original_name | VARCHAR(256) | |
| mime_type | VARCHAR(128) | |
| size | BIGINT | 声明大小 |
| access_level | VARCHAR(16) | |
| expires_at | TIMESTAMPTZ | 会话过期 |

索引：`idx_infra_file_upload_session_tenant_status`；`idx_infra_file_upload_session_expires`

**`infra_file_binding`**（业务关联）

| 列 | 类型 | 说明 |
|----|------|------|
| file_id | BIGINT | → infra_file.id |
| biz_type | VARCHAR(64) | 业务类型 |
| biz_id | BIGINT | 业务主键 |

索引：`idx_infra_file_binding_file`；`uk_infra_file_binding_biz`（tenant_id, biz_type, biz_id, file_id, deleted=0）

### D3：权限种子 ID 段

使用 `2000+` 段，避免与 system `1000+` 冲突：

```text
2000  infra（目录）
2100  infra:storage
2101  infra:storage:query
2102  infra:storage:update
2103  infra:storage:test
2200  infra:file
2201  infra:file:list
2202  infra:file:upload
2203  infra:file:download
2204  infra:file:delete
```

`sys_role_permission`：为 role_id=100 插入上述 permission id（2101–2104, 2201–2204 及目录节点按需；与 system 种子一致，目录 type=1 也绑定 super_admin）。

### D4：Codegen 表清单

```bash
./scripts/codegen.sh -m infra -t infra_storage_provider,infra_file,infra_file_upload_session,infra_file_binding --migrate
```

### D5：infra-biz pom 依赖

对齐 `system-biz`：

- `relayflow-spring-boot-starter-web`
- `relayflow-spring-boot-starter-security`
- `relayflow-spring-boot-starter-mybatis`
- `relayflow-spring-boot-starter-tenant`
- `relayflow-spring-boot-starter-oss`
- `relayflow-common`
- `spring-boot-starter-validation`
- `lombok` (provided)
- `build-helper-maven-plugin` → `target/generated-sources/mybatis`

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| codegen 需本地 PG | `--migrate` + compose；无库时 compile 仍可通过（空 generated 目录） |
| 权限 ID 冲突 | 固定 2000 段，文档注明 |
| config_json 无 DB 校验 | 业务层校验；V1 仅 JSON 文本 |

## Migration Plan

1. 应用 `V0.1.0.5`（新环境自动；已有环境 Flyway migrate）
2. codegen 合并 DO/Mapper
3. `infra-biz` pom 更新后 `mvn compile`

**回滚**：不删历史 Flyway；新表可空置，不影响现有 system 功能。

## 验证

```bash
openspec validate infra-storage-schema --strict
./scripts/codegen.sh -m infra -t infra_storage_provider,infra_file,infra_file_upload_session,infra_file_binding --migrate
./mvnw -pl relayflow-module-infra/relayflow-module-infra-biz -am compile
```
