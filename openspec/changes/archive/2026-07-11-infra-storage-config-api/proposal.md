# 提案：租户存储配置 API — infra-storage-config-api

## Why

`infra-storage-schema` 已创建 `infra_storage_provider` 表与 RBAC 种子，但管理端无法读写租户 MinIO 配置、也无法测试连通性。须在文件直传 API 之前提供存储配置后端。

## What Changes

- `StorageProviderService`：租户 provider CRUD、密钥 AES 加解密、`config_json` 序列化
- `GET/PUT/DELETE /admin-api/infra/storage/config`：查询/保存/删除租户存储配置（密钥脱敏）
- `POST /admin-api/infra/storage/test-connection`：探测 MinIO 连通性
- 删除前校验 `infra_file` 引用；切换默认 provider 时旧默认改 `legacy`
- `relayflow.encrypt.aes-key` 配置项 + `AesGcmEncryptor`
- `infra-api` 增加 `ErrorCodeConstants`

## Capabilities

### New Capabilities

（无新域）

### Modified Capabilities

- `infra`：租户存储配置管理 API 与密钥保护行为

## Impact

| 区域 | 影响 |
|------|------|
| `relayflow-module-infra-biz` | Controller / Service / Convert / VO |
| `relayflow-module-infra-api` | ErrorCodeConstants |
| `relayflow-common` 或 `starter-security` | AES 加解密工具 |
| `relayflow-server/application.yml` | `relayflow.encrypt.aes-key` |
| `deploy/.env.example` | 加密密钥 env |
| `web/` | **不改**（→ `infra-storage-config-web`） |

## 不在本 change

- 管理端存储设置页 UI
- Upload Session / 文件下载 API

## 前置

- ① `infra-storage-platform`、② `infra-storage-schema` 已实施
- 父史诗：[`infra-storage-v1`](../infra-storage-v1/design.md) §D2、③
