# 提案：基础设施存储 V1 史诗（infra-storage-v1）

## Why

系统管理 V1 已闭环，但 **对象存储与文件能力几乎空白**：`starter-oss`、`infra_*` 表、文件 API、管理端 `/admin/infra/file` 均为占位。后续 IM 附件、头像、文档、导入包等均依赖统一的 **可配置存储 + 前端直传 + 元数据登记 + 业务绑定** 能力。

本 change 为 **规划型史诗**：定义存储 V1 的策略架构、直传流程、租户多 provider 配置、下载分流与子 change 拆分；**不在本 change 内写业务代码**。

## What Changes

- 发布史诗级 `design.md`：策略模式 `ObjectStorageClient`、Upload Session 直传、Confirm 登记、`storage_uri` 多 provider 读取、启动校验、public/private 下载
- 在 `infra` 域 spec 增量中补充/修订存储相关 **运行时行为**
- 定义 **8 个 implementation change**（每个 ≤10 tasks，降低 AI 执行出错率）：
  1. `infra-storage-platform` — `[平台]` starter-oss 策略骨架 + 启动默认存储校验（仅 MinIO 实现）
  2. `infra-storage-schema` — `[平台]` Flyway `infra_*` 表 + 权限种子 + codegen
  3. `infra-storage-config-api` — 租户存储配置 CRUD + 测试连接 API
  4. `infra-storage-config-web` — 管理端存储设置页（`-web` 先行）
  5. `infra-file-upload-api` — Upload Session + Confirm + `infra_file` 服务
  6. `infra-file-web` — 文件管理页 + 前端直传 composable
  7. `infra-file-download-api` — public / private 下载端点
  8. `infra-file-integrate` — 联调、看板、去 Mock
- **Supersedes / 吸收**：
  - `tenant-ready-foundation` §5.4 MinIO 前缀 → 并入本史诗 objectKey 规范
  - 原 `openspec/specs/infra`「经 API 上传文件字节」→ 修订为「Presigned 直传 + Confirm」
- V1 仅实现 **MinIO** provider；接口与工厂预留 S3/OSS/COS 扩展位

## Capabilities

### New Capabilities

（无独立新域；行为增量写入 `infra`）

### Modified Capabilities

- `infra`：可配置租户存储、启动默认存储校验、Presigned 直传与 Confirm、文件元数据与业务绑定、public/private 下载分流

## Impact

| 区域 | 影响 |
|------|------|
| `relayflow-dependencies` | MinIO Java SDK 版本入 BOM |
| `relayflow-framework/relayflow-spring-boot-starter-oss` | `ObjectStorageClient` 策略、`MinioObjectStorageClient`、启动校验 |
| `relayflow-module-infra-biz` | 存储配置、文件 Session/Confirm、下载 Controller |
| `relayflow-module-infra-api` | `FileApi` 供 system/im 跨域引用 fileId |
| `relayflow-server` | `application.yml` 增加 `relayflow.storage.*`；Flyway `infra_*` |
| `web/` | `/admin/infra/storage`、`/admin/infra/file` 接 API + 直传 |
| `deploy/.env.example` | 补充 `RELAYFLOW_STORAGE_DEFAULT_PROVIDER` 等 |
| `openspec/lanes/` | 各子 change contract |
| `docs/dev/api-integration-board.md` | 子 change 实施时更新 |

**本 change 不写 Java / web 代码。**

## 不在本 change 范围

- OSS / COS / 阿里 PostObject 等第二家云厂商实现（仅预留策略接口）
- 分片上传 Multipart、STS 临时凭证、跨云自动迁移任务
- `local` 作为生产默认存储（dev profile 可选，生产禁止）
- WebSocket、审计日志（仍属 infra 域其他需求，另开 change）
- 各业务域（IM 头像、消息附件）引用 fileId — 本史诗提供 `FileApi`，业务切片后续对接

## 前置条件

- `system-admin-v1` 已归档（RBAC、管理端壳层可用）
- Docker Compose 已含 MinIO（`deploy/compose.yml`）
- `tenant-ready-foundation` §2–4 已落地（JWT `tenant_id`、MyBatis 租户行）
