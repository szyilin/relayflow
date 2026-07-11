# API 契约：infra-storage-config

> **状态**：已冻结（2026-07-11）  
> **起草**：`infra-storage-config-web` change（API 由 `infra-storage-config-api` 实现）  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)

## 背景

管理端租户对象存储配置：读写 `infra_storage_provider`，V1 仅 MinIO。

## 端点

### GET /admin-api/infra/storage/config

| 项 | 值 |
|----|-----|
| 鉴权 | Bearer JWT |
| 所需权限 | `infra:storage:query` |

**Response `data`**：

```json
{
  "providers": [
    {
      "provider": "minio",
      "status": "active",
      "isDefault": true,
      "endpoint": "http://127.0.0.1:9000",
      "bucket": "relayflow",
      "accessKey": "minioadmin",
      "useSsl": false,
      "pathPrefix": "",
      "secretKeyConfigured": true
    }
  ]
}
```

空 `providers` 表示租户未自定义，运行时回退 `application.yml` Bootstrap。

### PUT /admin-api/infra/storage/config

| 项 | 值 |
|----|-----|
| 鉴权 | Bearer JWT |
| 所需权限 | `infra:storage:update` |

**Request body**：

```json
{
  "provider": "minio",
  "endpoint": "http://127.0.0.1:9000",
  "bucket": "relayflow",
  "accessKey": "minioadmin",
  "secretKey": "minioadmin",
  "useSsl": false,
  "pathPrefix": "",
  "isDefault": true
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| `provider` | 是 | V1 仅 `minio` |
| `endpoint` / `bucket` / `accessKey` | 是 | |
| `secretKey` | 新建必填；更新可空（保留原密文） | |
| `isDefault` | 否 | `true` 时旧默认改 `legacy` |

**Response `data`**：`true`

### DELETE /admin-api/infra/storage/config

| 项 | 值 |
|----|-----|
| Query | `provider=minio` |
| 所需权限 | `infra:storage:update` |

**Response `data`**：`true`

**拒绝删除**：`2002001002` — provider 仍被 `infra_file` 引用

### POST /admin-api/infra/storage/test-connection

| 项 | 值 |
|----|-----|
| 所需权限 | `infra:storage:test` |

**Request body**（二选一）：

```json
{ "provider": "minio" }
```

或带 inline 参数（须含 `secretKey`）：

```json
{
  "provider": "minio",
  "endpoint": "http://127.0.0.1:9000",
  "bucket": "relayflow",
  "accessKey": "minioadmin",
  "secretKey": "minioadmin",
  "useSsl": false,
  "pathPrefix": ""
}
```

**Response `data`**：`true`；失败 `2002001003`

## curl 示例

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/admin-api/system/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.data.accessToken')

curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/admin-api/infra/storage/config | jq

curl -s -X PUT -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"provider":"minio","endpoint":"http://127.0.0.1:9000","bucket":"relayflow","accessKey":"minioadmin","secretKey":"minioadmin","isDefault":true}' \
  http://localhost:8080/admin-api/infra/storage/config | jq

curl -s -X POST -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"provider":"minio"}' \
  http://localhost:8080/admin-api/infra/storage/test-connection | jq
```

## 前端约定

| 项 | 约定 |
|----|------|
| API | `web/src/api/admin/storage.ts` |
| Store | `stores/storage.ts`；**无 Mock 回退** |
| 页面 | `/admin/infra/storage` |
| 权限 | `infra:storage:query/update/test` |
