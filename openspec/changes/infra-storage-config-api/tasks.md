# 任务：infra-storage-config-api

> **Lane**：`[平台]` 后端 · 无 UI。  
> **前置**：①② 已实施；阅读本 change `design.md` 与父史诗 §D2。

## 前置

- [x] 0.1 阅读 `design.md` 与 [`infra-storage-v1/design.md`](../infra-storage-v1/design.md) §D2

## 加密基座

- [x] 1.1 `relayflow-common`：`AesGcmEncryptor`
- [x] 1.2 `starter-security`：`EncryptProperties` + `EncryptAutoConfiguration`；`application.yml` / `.env.example` 增加 `relayflow.encrypt.aes-key`

## 业务 API

- [x] 2.1 `infra-api`：`ErrorCodeConstants`（`2_002_001_xxx`）
- [x] 2.2 `StorageProviderService`：CRUD、密钥加解密、`config_json`、默认切换、删除引用校验
- [x] 2.3 `StorageConfigController`：`GET/PUT/DELETE /admin-api/infra/storage/config` + `POST test-connection` + `@PreAuthorize`
- [x] 2.4 VO / Convert（响应脱敏）

## 验证

- [x] 3.1 `./mvnw -pl relayflow-server -am compile`
- [x] 3.2 `openspec validate infra-storage-config-api --strict`

## 不在本 change

- `web/` 存储设置页（→ `infra-storage-config-web`）
- Upload Session / 文件下载
