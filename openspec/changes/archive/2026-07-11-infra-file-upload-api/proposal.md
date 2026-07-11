# 提案：文件直传 API — infra-file-upload-api

## Why

`infra-storage-schema` 已创建 `infra_file` / `infra_file_upload_session` 表，`infra-storage-platform` 已提供 MinIO presigned PUT/HEAD，但管理端仍无法创建上传会话、确认直传结果并落库 `fileId`。须在文件管理 UI（⑥）之前提供 Upload Session + Confirm 后端。

## What Changes

- 起草 `openspec/lanes/infra-file-upload/contract.md`（直传三阶段 API 契约）
- `FileUploadSessionService`：创建会话（presigned PUT）、确认（HeadObject 校验 + 写 `infra_file`）
- `FileService`：文件元数据持久化、`getFile`、`bindFile`
- `POST /admin-api/infra/file/upload-session`、`POST /admin-api/infra/file/confirm`
- `FileApi`（`infra-api`）供跨域 `getFile` / `bindFile`
- `StorageProviderService` 增加运行时有效 provider 配置解析（Bootstrap / 租户默认）
- `ErrorCodeConstants` 增加 `2_002_002_xxx` 文件域错误码

## Capabilities

### New Capabilities

（无新域）

### Modified Capabilities

- `infra`：Presigned 直传 Upload Session + Confirm 行为，替代 spec 中「经 API 上传字节」的笼统描述

## Impact

| 区域 | 影响 |
|------|------|
| `relayflow-module-infra-biz` | File Controller / Service / ApiImpl / VO |
| `relayflow-module-infra-api` | `FileApi`、DTO、`ErrorCodeConstants` |
| `openspec/lanes/infra-file-upload/` | contract.md |
| `web/` | **不改**（→ `infra-file-web`） |

## 不在本 change

- 文件列表 / 删除 UI、`useDirectUpload` composable（→ `infra-file-web`）
- public/private 下载 302（→ `infra-file-download-api`）
- 孤儿对象定时清理（V1.1）

## 前置

- ① `infra-storage-platform`、② `infra-storage-schema`、③ `infra-storage-config-api` 已实施
- 父史诗：[`infra-storage-v1`](../infra-storage-v1/design.md) §D3–D4、⑤
