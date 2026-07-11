# 提案：存储平台层 — starter-oss（infra-storage-platform）

## Why

`infra-storage-v1` 史诗要求可插拔 `ObjectStorageClient` 与启动时默认存储校验，但 `relayflow-spring-boot-starter-oss` 仍为空壳。须先在 framework 层落地策略接口与 MinIO 实现，供后续 `infra-biz` 文件/配置 API 复用。

## What Changes

- BOM 引入 MinIO Java SDK
- `ObjectStorageProviderType` 枚举（V1 仅 `MINIO` 有实现）
- `ObjectStorageClient` 接口 + `MinioObjectStorageClient`
- `ObjectStorageClientFactory` + `OssAutoConfiguration`
- `StorageProperties`（`relayflow.storage.*`）+ `StorageBootstrapValidator`
- `application.yml` / `deploy/.env.example` 补充存储配置项

## Capabilities

### New Capabilities

（无新域）

### Modified Capabilities

- `infra`：策略扩展架构、Bootstrap 默认存储校验（平台层落地）

## Impact

| 区域 | 影响 |
|------|------|
| `relayflow-dependencies` | `io.minio:minio` 版本 |
| `relayflow-framework/relayflow-spring-boot-starter-oss` | 核心实现 |
| `relayflow-server` | `application.yml` 增加 `relayflow.storage.*` |
| `deploy/.env.example` | 存储相关 env |
| `web/` | **不改** |

## 不在本 change

- Flyway / `infra_*` 表（→ `infra-storage-schema`）
- 管理端 API 与 UI
- Upload Session / Confirm 业务 API

## 前置

- 史诗：[`infra-storage-v1`](../infra-storage-v1/design.md) §D1、D2、①
