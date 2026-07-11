# 设计：文件直传 API — infra-file-upload-api

## Context

- 父史诗 §D3–D4：直传三阶段 + `infra_file` / `infra_file_binding`
- 前置：starter-oss `createPresignedPut` / `headObject`；`infra_file_upload_session` 表与 codegen 已就绪；租户存储配置 API 可解析 Bootstrap / 租户有效源
- Lane：`[平台]` 纯后端；contract 由本 change 起草供 ⑥ 对接

## Goals / Non-Goals

**Goals:**

- 管理端创建上传会话：生成 `objectKey`、写入 `pending` session、返回 presigned PUT
- 确认上传：HeadObject 校验存在与 size → INSERT `infra_file` → session 改 `confirmed` → 返回 `fileId` + `storageUri`
- 跨域 `FileApi.getFile` / `bindFile`
- objectKey：`tenant/{tenantId}/files/{yyyy}/{mm}/{uuid}{ext}`
- session TTL 15 分钟

**Non-Goals:**

- 文件列表、删除、下载 API
- 分片上传、POST Policy 模式（`mode` 字段预留 `post_policy`）
- 前端 `useDirectUpload`

## Decisions

### D1：API 形状

| Method | Path | Permission |
|--------|------|------------|
| POST | `/admin-api/infra/file/upload-session` | `infra:file:upload` |
| POST | `/admin-api/infra/file/confirm` | `infra:file:upload` |

**upload-session request**：`filename`, `size`, `mimeType`, `accessLevel`（`public` \| `private`，默认 `private`）

**upload-session response**：`uploadId`, `mode: "presigned_put"`, `objectKey`, `uploadUrl`, `headers`, `expiresAt`

**confirm request**：`uploadId`, `etag`（可选）, `size`

**confirm response**：`fileId`, `storageUri`（`minio://{bucket}/{objectKey}`）

### D2：运行时 provider 解析

`StorageProviderService.resolveEffectiveProviderConfig()`：

1. 租户 `infra_storage_provider` 存在 `is_default=1` → 解密租户 MinIO 配置
2. 否则 → `storageProperties.toBootstrapConfig()`

上传 session 记录 `provider` 字段（V1 固定 `minio`），confirm 时按 session.provider 解析（与有效源一致）。

### D3：Confirm 校验

1. session 存在且 `status=pending`
2. `expires_at > now()`，否则标记 `expired` 并拒绝
3. `headObject` 非空
4. `headObject.size` 与 request `size` 一致（±0）
5. request `etag` 非空时与 head etag 规范化后比对（去引号）

### D4：错误码（`2_002_002_xxx`）

| Code | 含义 |
|------|------|
| `2_002_002_001` | 上传会话不存在 |
| `2_002_002_002` | 上传会话已过期 |
| `2_002_002_003` | 上传会话状态无效 |
| `2_002_002_004` | 对象存储中未找到已上传对象 |
| `2_002_002_005` | 确认时文件大小不匹配 |
| `2_002_002_006` | 文件不存在 |
| `2_002_002_007` | 上传请求参数无效 |

### D5：FileApi

```java
FileRespDTO getFile(Long fileId);
void bindFile(FileBindReqDTO request);
```

`bindFile` 写入 `infra_file_binding`（`biz_type`, `biz_id`）；同租户、同 file 可幂等跳过重复绑定。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 未 confirm 孤儿对象 | session TTL 15min；V1.1 定时清理 |
| CORS 阻断浏览器 PUT | deploy 文档 + ⑥ 浏览器验收 |
| 有效源切换后会话 provider 不一致 | session 创建时固化 provider 与 objectKey |

## 验证

```bash
./mvnw -pl relayflow-server -am compile
# 登录 → upload-session → curl PUT presigned → confirm
```
