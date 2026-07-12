# 提案：存储域表结构 — infra-storage-schema

## Why

`infra-storage-platform` 已落地 `starter-oss` 策略骨架，但 **infra 域尚无 `infra_*` 存储表**、`infra-biz` 无 MyBatis/Security starters，后续存储配置 API 与文件直传 API 无法持久化。须在业务 API 之前完成 Flyway、权限种子与 codegen 基座。

## What Changes

- Flyway `V0.1.0.5__init_infra_storage.sql`：创建 `infra_storage_provider`、`infra_file`、`infra_file_upload_session`、`infra_file_binding`
- 同脚本种子 `infra:storage:*`、`infra:file:*` 权限点并绑定 `super_admin`
- `./scripts/codegen.sh --module infra --tables …` 生成 DO/Mapper 合并至 `infra-biz`
- `relayflow-module-infra-biz/pom.xml` 引入 web/security/mybatis/tenant/oss starters + build-helper

## Capabilities

### New Capabilities

（无新域）

### Modified Capabilities

- `infra`：存储与文件元数据表结构、RBAC 权限点种子

## Impact

| 区域 | 影响 |
|------|------|
| `relayflow-server/.../db/migration/` | 新增 `V0.1.0.5` |
| `relayflow-module-infra-biz` | pom starters、generated-sources |
| `sys_permission` / `sys_role_permission` | 种子数据（租户 1） |
| `web/` | **不改** |

## 不在本 change

- 存储配置 / 文件 Upload Session / 下载 Controller（→ ③–⑦ 子 change）
- 密钥加解密 Service 实现

## 前置

- ① `infra-storage-platform` 已实施
- 父史诗：[`infra-storage-v1`](../infra-storage-v1/design.md) §D2、D3、D4、D7、②
