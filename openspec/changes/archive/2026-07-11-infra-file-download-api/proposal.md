## Why

文件元数据与直传已就绪，但缺少 public/private 下载分流：公开文件需免登录 302 presigned GET，私有文件需管理端鉴权后短时签名。

## What Changes

- `GET /app-api/infra/file/public/{fileId}` — 校验 `access_level=public`，302 + 长缓存
- `GET /admin-api/infra/file/{fileId}/download` — `infra:file:download`，302 presigned GET（15min）
- `FileDownloadService` + `StorageProviderService.resolveProviderConfig(provider)`
- Security 白名单：公开下载端点 `permitAll`

## Capabilities

### Modified Capabilities

- `infra`：文件下载分流（public app 端点 + private admin 端点）

## Impact

| 范围 | 说明 |
|------|------|
| `relayflow-module-infra-biz` | 下载 Service + app/admin Controller |
| `relayflow-spring-boot-starter-security` | 公开端点白名单 |
