# 设计：存储平台层 — starter-oss（infra-storage-platform）

## Context

- 父史诗：[`infra-storage-v1`](../infra-storage-v1/design.md)
- 现状：`starter-oss` 仅有 `pom.xml` + `package-info.java`

## Goals / Non-Goals

**Goals:**

- 定义稳定策略接口，V1 实现 MinIO
- 启动时校验 `default-provider` 与 MinIO 配置块
- presign PUT/GET、head、delete、connectivity check

**Non-Goals:**

- 租户 DB 配置读取（→ `infra-storage-config-api`）
- S3/OSS 具体实现类

## Decisions

### 包结构

```text
com.relayflow.framework.oss
  ├── config/
  │     StorageProperties
  │     OssAutoConfiguration
  │     StorageBootstrapValidator
  ├── core/
  │     ObjectStorageProviderType
  │     ObjectStorageClient
  │     ObjectStorageClientFactory
  │     model/ PresignedUpload, StorageObjectMeta, StorageProviderConfig
  └── minio/
        MinioObjectStorageClient
```

### StorageProperties 形状

```yaml
relayflow:
  storage:
    default-provider: minio   # 必填
    allow-local: false        # dev 可 true
    validate-on-startup: true
    minio:
      endpoint: http://127.0.0.1:9000
      access-key: minioadmin
      secret-key: minioadmin
      bucket: relayflow
      use-ssl: false
      path-prefix: ""         # 可选，最终 key = pathPrefix + tenant/...
```

### ObjectStorageClient 方法（V1）

```java
void checkConnectivity(StorageProviderConfig config);
PresignedUpload createPresignedPut(StorageProviderConfig config, String objectKey, String contentType, Duration ttl);
StorageObjectMeta headObject(StorageProviderConfig config, String objectKey);
void deleteObject(StorageProviderConfig config, String objectKey);
String createPresignedGet(StorageProviderConfig config, String objectKey, Duration ttl);
```

`StorageProviderConfig` 为 POJO，与 DB JSON 解耦，便于 Factory 统一入参。

### Factory 行为

```java
switch (type) {
  case MINIO -> new MinioObjectStorageClient();
  case S3, OSS, COS, LOCAL -> throw new UnsupportedStorageProviderException(type);
}
```

### Bootstrap 校验

`StorageBootstrapValidator` implements `ApplicationRunner` 或 `@PostConstruct` in AutoConfiguration:

1. `default-provider` blank → `IllegalStateException`
2. non-dev + `local` → fail
3. minio block incomplete → fail
4. if `validate-on-startup` → `factory.getClient(MINIO, bootstrapConfig).checkConnectivity()`

### BOM

在 `relayflow-dependencies/pom.xml` 的 `dependencyManagement` 增加 `io.minio:minio`（版本与 Spring Boot 3.4 / Java 21 兼容，如 8.5.x）。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| CI 无 MinIO | `validate-on-startup: false` in test profile |
| MinIO SDK API 变更 | 版本锁在 BOM |

## 验证

```bash
./mvnw -pl relayflow-server -am compile
# 本地有 MinIO 时启动 server，应通过 bootstrap 校验
```
