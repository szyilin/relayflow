# 任务：infra-file-download-api

> **Lane**：`[平台]` 后端 · 无 UI。

## 实现

- [x] 1.1 `StorageProviderService.resolveProviderConfig(provider)`
- [x] 1.2 `FileDownloadService`（public / admin 分流 + presigned GET）
- [x] 1.3 `AppFileController` + `FileController` 下载端点
- [x] 1.4 Security `permitAll` 公开端点
- [x] 1.5 `./mvnw -pl relayflow-server -am compile` + curl 验收

## 验证

- [x] 2.1 `openspec validate infra-file-download-api --strict`
