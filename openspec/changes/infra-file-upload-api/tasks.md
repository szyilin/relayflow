# 任务：infra-file-upload-api

> **Lane**：`[平台]` 后端 · 无 UI。  
> **前置**：①②③ 已实施；阅读本 change `design.md` 与父史诗 §D3–D4。

## 前置

- [x] 0.1 阅读 `design.md` 与 [`infra-storage-v1/design.md`](../infra-storage-v1/design.md) §D3–D4

## Contract

- [x] 1.1 起草 `openspec/lanes/infra-file-upload/contract.md`

## 存储运行时

- [x] 2.1 `StorageProviderService.resolveEffectiveProviderConfig()`（Bootstrap / 租户有效源）

## 业务 API

- [x] 3.1 `infra-api`：`ErrorCodeConstants`（`2_002_002_xxx`）、`FileApi` + DTO
- [x] 3.2 `FileService`：`getFile`、`createFromSession`、`bindFile`
- [x] 3.3 `FileUploadSessionService`：create + confirm（HeadObject 校验）
- [x] 3.4 `FileController`：`POST upload-session` / `POST confirm` + `@PreAuthorize`
- [x] 3.5 `FileApiImpl` 委托 `FileService`

## 验证

- [x] 4.1 `./mvnw -pl relayflow-server -am compile`
- [x] 4.2 `openspec validate infra-file-upload-api --strict`

## 不在本 change

- `web/` 文件页与 `useDirectUpload`（→ `infra-file-web`）
- 下载分流 API（→ `infra-file-download-api`）
