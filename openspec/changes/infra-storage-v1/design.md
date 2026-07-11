# 设计：基础设施存储 V1 史诗（infra-storage-v1）

## Context

- **上游**：`system-admin-v1`（RBAC + 管理端）、`tenant-ready-foundation` §2–4（租户上下文）
- **参考**：WeKnora 租户 `storage_engine_config` + `FileService` 工厂；**不照搬**其后端代理上传，采用 Presigned 直传
- **现状**：`starter-oss` 空壳；无 `infra_*` 表；`/admin/infra/file` 占位；spec 仍写「经 API 上传字节」

## Goals / Non-Goals

**Goals:**

- **策略架构**：`ObjectStorageClient` 接口 + `ObjectStorageClientFactory`；V1 仅 `MinioObjectStorageClient`
- **启动强制默认存储**：`relayflow.storage.default-provider` 必填（V1 仅 `minio`）；对应配置块完整否则 **启动失败**；生产 profile 禁止 `local`
- **租户可配置多 provider**：`infra_storage_provider` 表存各 provider 配置（密钥加密）；一个 `default_provider`；历史文件按 `storage_uri` 读对应 provider
- **直传三阶段**：`upload-session` → 浏览器 PUT presigned → `confirm` → 返回 `fileId`
- **业务解耦**：各域 API 只收 `fileId`；`infra_file_binding` 关联业务实体
- **下载分流**：`public`（头像等）走公开端点 + 长缓存；`private` 鉴权后 302 presigned GET
- **objectKey 规范**：`tenant/{tenantId}/files/{yyyy}/{mm}/{uuid}{ext}`

**Non-Goals:**

- V1 实现 OSS/COS/S3 第二套签名（接口预留，`UnsupportedStorageProviderException` 明确报错）
- 分片上传、上传回调 Webhook、CDN 集成
- 跨 provider 自动迁移后台任务
- `infra` 审计日志、WebSocket（另开 change）

## Decisions

### D1：策略模式入口（扩展性）

```text
ObjectStorageProviderType   enum: MINIO, S3, OSS, COS, LOCAL（后四者 V1 仅枚举占位）

ObjectStorageClient         interface
  checkConnectivity()
  createPresignedPut(session) → PresignedUpload
  headObject(storageUri)
  deleteObject(storageUri)
  createPresignedGet(storageUri, ttl) → url

ObjectStorageClientFactory
  getClient(providerType, StorageProviderConfig) → ObjectStorageClient
  V1 switch 仅 MINIO 有实现；其他 throw 明确异常

MinioObjectStorageClient    implements ObjectStorageClient
```

**理由**：业务与 Controller **只依赖接口 + Factory**，新增阿里云时加 `OssObjectStorageClient` + Factory 分支，不改 Upload/Confirm 主流程。

**替代**：按厂商各写 Controller — 拒绝，维护成本高。

### D2：配置两层

| 层 | 存储 | 用途 |
|----|------|------|
| **Bootstrap** | `application.yml` `relayflow.storage.*` | 部署默认；租户未自定义时使用；**启动校验** |
| **Tenant** | `infra_storage_provider` 多行（每 provider 一条） | 管理后台可增改；`status`: `active` / `legacy` |

租户表字段（每 provider 一行）：

```text
id, tenant_id, provider (minio), status, is_default,
config_json (endpoint, bucket, access_key, secret_key_enc, use_ssl, path_prefix),
create_time, update_time, deleted
```

- `secret_key` **AES 加密**存储；API 响应不回显明文
- 切换默认：新 `is_default=true`，旧 provider 改 `legacy`（配置保留供历史文件读取）
- 删除 provider：若仍有 `infra_file.provider` 引用 → 拒绝删除

Bootstrap 校验（`StorageBootstrapValidator`）：

1. `default-provider` 非空
2. V1 允许值：`minio`（`spring.profiles.active=dev` 可额外允许 `local` 且 `allow-local=true`）
3. 对应 `relayflow.storage.minio.*` 必填
4. 可选 `validate-on-startup=true` 时 HeadBucket

### D3：Upload Session 统一协议（前后端契约）

**创建会话** `POST /admin-api/infra/file/upload-session`

```json
// request
{ "filename": "a.pdf", "size": 1024, "mimeType": "application/pdf", "accessLevel": "private" }

// response
{
  "uploadId": "…",
  "mode": "presigned_put",
  "objectKey": "tenant/1/files/2026/07/uuid.pdf",
  "uploadUrl": "https://minio…",
  "headers": { "Content-Type": "application/pdf" },
  "expiresAt": "…"
}
```

**确认** `POST /admin-api/infra/file/confirm`

```json
// request
{ "uploadId": "…", "etag": "\"abc\"", "size": 1024 }

// response
{ "fileId": "…", "storageUri": "minio://bucket/tenant/1/files/…" }
```

`infra_file_upload_session` 表：`status` = `pending` | `confirmed` | `expired`。

Confirm 时：HeadObject 校验 size/存在 → INSERT `infra_file` → 更新 session。

**补偿**：定时任务清理超时 `pending` session 对应对象（V1 可简化为 confirm 失败不删，文档注明 V1.1 清理）。

### D4：文件元数据与业务绑定

**`infra_file`**

```text
id, tenant_id, provider, storage_uri, object_key,
original_name, mime_type, size, sha256, access_level (public|private),
creator, create_time, updater, update_time, deleted
```

**`infra_file_binding`**（业务关联，可选 confirm 时或业务 API 写入）

```text
id, tenant_id, file_id, biz_type, biz_id, create_time
```

跨域模块通过 `FileApi.getFile(fileId)` / `bindFile(fileId, bizType, bizId)` 访问。

### D5：读取解析（多 provider 历史兼容）

```text
resolveClientForFile(file):
  provider = file.provider  // 或 parse storage_uri scheme
  config = tenantStorageService.getProviderConfig(tenantId, provider)
  if config == null → FileStorageUnavailableException
  return factory.getClient(provider, config)
```

与 WeKnora `resolveFileServiceForPath` 同思路；**删除 provider 配置 = 该批文件不可读**（产品预期）。

### D6：下载

| access_level | 端点 | 行为 |
|--------------|------|------|
| `public` | `GET /app-api/infra/file/public/{fileId}` | 不强制登录；校验 public；302 presigned GET；`Cache-Control: public, max-age=31536000` |
| `private` | `GET /admin-api/infra/file/{fileId}/download` | JWT + `infra:file:download` 或 binding 业务鉴权；302 presigned GET TTL 15min |

V1 不做全量代理流式下载（大文件压 Java）；合规场景 V1.1 可加 proxy 模式。

### D7：权限点（Flyway 种子）

```text
infra:storage:query | infra:storage:update | infra:storage:test
infra:file:list | infra:file:upload | infra:file:download | infra:file:delete
```

### D8：前端直传 composable

`composables/useDirectUpload.ts`：

1. `createUploadSession(file)` → API
2. `fetch(uploadUrl, { method: 'PUT', body: file, headers })` → 读 ETag
3. `confirmUpload({ uploadId, etag, size })` → fileId

**禁止**页面直接 import MinIO SDK 或持有 AK/SK。

### D9：子 Change 拆分原则

- 每个 change **≤10 checkbox tasks**
- `[平台]` 无 UI：单 change 后端先行
- 有 UI：`-web` → `-api` → `-integrate`（存储配置、文件管理各一组）
- 平台层（starter + schema）先于业务 API

## 子 Change 路线图

```text
infra-storage-v1（本 change — 仅文档）
  │
  ├─① infra-storage-platform      [平台] 策略接口 + MinIO 实现 + 启动校验
  ├─② infra-storage-schema        [平台] Flyway + 权限种子 + codegen
  ├─③ infra-storage-config-api    租户存储配置 API
  ├─④ infra-storage-config-web    存储设置 UI（可并行 ③ 后）
  ├─⑤ infra-file-upload-api       Session + Confirm + FileService
  ├─⑥ infra-file-web              文件页 UI + useDirectUpload
  ├─⑦ infra-file-download-api     public/private 下载
  └─⑧ infra-file-integrate        联调 + 看板 + 史诗归档前置
```

```text
实施顺序：① → ② → ③ → ④
                    ↘ ⑤ → ⑥ → ⑦ → ⑧
```

③④ 可与 ⑤ 串行（⑤ 依赖 ①②）；⑥ 依赖 ⑤ 的 contract；⑧ 依赖 ③④⑥⑦。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 前端直传 CORS | MinIO bucket CORS 在 deploy 文档 + 测试连接时校验 |
| 未 confirm 孤儿对象 | session 过期清理（V1.1）；限制 session TTL 15min |
| 密钥泄露 | 加密存储 + 管理 API 不回显 + RBAC |
| 仅 MinIO 与 future OSS 签名差异 | UploadSession `mode` 字段预留 `post_policy` |
| 启动校验阻塞 CI | `validate-on-startup=false` 用于无 MinIO 的 compile-only CI |

## Migration Plan

1. 审阅并 validate 本史诗
2. 按序 propose 8 个子 change（各含 proposal/design/tasks/spec delta）
3. 实施 ①② → ③④∥⑤ → ⑥⑦ → ⑧
4. 更新 `AGENTS.md`「下一优先」；archive 本史诗；同步 `openspec/specs/infra/spec.md`
5. 勾选 `tenant-ready-foundation` §5.4 由平台组完成

**回滚**：新表可保留；关闭 `relayflow.storage` 相关端点；前端回 Mock（integrate 前无影响）。

## Open Questions

- dev profile 是否允许 `local` provider？（设计倾向：仅 `dev` + `allow-local=true`）
- `infra-file-integrate` 是否合并 ③④ 的 web 联调？（是，⑧ 统一验收）

## 参考

- WeKnora：`internal/application/service/file/factory.go`、`StorageEngineSettings.vue`
- 主 spec：`openspec/specs/infra/spec.md`
- 租户前缀：`tenant-ready-foundation` design D6
