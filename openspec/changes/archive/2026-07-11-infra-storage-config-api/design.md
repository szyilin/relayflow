# 设计：租户存储配置 API — infra-storage-config-api

## Context

- 父史诗 §D2：Bootstrap（`application.yml`）+ Tenant（`infra_storage_provider`）
- 前置：starter-oss `ObjectStorageClient.checkConnectivity`、表与权限种子已就绪
- Lane：`[平台]` 纯后端，无 `-web` contract

## Goals / Non-Goals

**Goals:**

- 租户可 CRUD MinIO provider 配置；`secret_key` AES-GCM 加密存 `config_json.secret_key_enc`
- API 响应不回显明文密钥；更新时 `secretKey` 空则保留原密文
- 切换 `isDefault=true` 时：新记录默认，旧默认改 `status=legacy`
- 删除 provider 前检查 `infra_file.provider` 引用数
- `test-connection` 支持提交参数或已保存配置

**Non-Goals:**

- OSS/S3 第二家实现
- 菜单种子、前端页面

## Decisions

### D1：API 形状

| Method | Path | Permission |
|--------|------|------------|
| GET | `/admin-api/infra/storage/config` | `infra:storage:query` |
| PUT | `/admin-api/infra/storage/config` | `infra:storage:update` |
| DELETE | `/admin-api/infra/storage/config?provider=minio` | `infra:storage:update` |
| POST | `/admin-api/infra/storage/test-connection` | `infra:storage:test` |

GET 返回 `{ "providers": [ StorageProviderRespVO ] }`；空列表表示租户未自定义，运行时回退 Bootstrap。

### D2：`config_json` 持久化格式

```json
{
  "endpoint": "http://127.0.0.1:9000",
  "bucket": "relayflow",
  "access_key": "minioadmin",
  "secret_key_enc": "<base64 ciphertext>",
  "use_ssl": false,
  "path_prefix": ""
}
```

### D3：密钥加解密

- `AesGcmEncryptor` in `relayflow-common`（纯工具）
- `EncryptProperties`：`relayflow.encrypt.aes-key`（Base64，32 字节 AES-256）
- Bean 注册在 `starter-security` `EncryptAutoConfiguration`

### D4：运行时解析

`StorageProviderConfig` 构建：

```java
ObjectStorageProviderType.MINIO + decrypt(secret_key_enc) + 其他字段
clientFactory.getClient(MINIO).checkConnectivity(config);
```

V1 `provider` 字段仅接受 `minio`。

### D5：错误码（`2_002_001_xxx`）

| Code | 含义 |
|------|------|
| `2_002_001_001` | provider 不存在 |
| `2_002_001_002` | provider 仍被文件引用 |
| `2_002_001_003` | 连通性测试失败 |
| `2_002_001_004` | V1 不支持的 provider 类型 |

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 加密密钥轮换 | V1 文档注明需备份；轮换需重加密（V1.1） |
| dev 无 aes-key | 提供 dev 默认 key + `.env.example` |

## 验证

```bash
./mvnw -pl relayflow-server -am compile
# 登录后 curl GET/PUT/POST test-connection
```
