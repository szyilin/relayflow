# 任务：infra-storage-platform

> **Lane**：`[平台]` 后端 · 无 UI。  
> **前置**：阅读 [`infra-storage-v1/design.md`](../infra-storage-v1/design.md) §D1、D2。

## 前置

- [ ] 0.1 阅读本 change `design.md` 与父史诗 `infra-storage-v1/design.md`

## 后端（relayflow-framework）

- [ ] 1.1 BOM：`relayflow-dependencies` 增加 `io.minio:minio`；`starter-oss` 引入该依赖
- [ ] 1.2 `ObjectStorageProviderType`、`StorageProviderConfig`、`PresignedUpload`、`StorageObjectMeta` 模型类
- [ ] 1.3 `ObjectStorageClient` 接口 + `UnsupportedStorageProviderException`
- [ ] 1.4 `MinioObjectStorageClient`（connectivity / presignPut / presignGet / head / delete）
- [ ] 1.5 `ObjectStorageClientFactory`（V1 仅 MINIO 分支）
- [ ] 1.6 `StorageProperties` + `OssAutoConfiguration` 注册 Factory 与 bootstrap config Bean
- [ ] 1.7 `StorageBootstrapValidator` 启动校验 + `relayflow-server/application.yml` + `deploy/.env.example`
- [ ] 1.8 `./mvnw -pl relayflow-server -am compile`（`validate-on-startup=false` 可在 test profile 关闭）

## 归档

- [ ] 2.1 `openspec validate infra-storage-platform --strict`
- [ ] 2.2 父史诗 `infra-storage-v1/tasks.md` 勾选 ① 立项完成（本 change 实施完成后）

## 不在本 change

- Flyway、`infra-biz` Controller（→ `infra-storage-schema` 及后续）
- `web/`
