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
  "effectiveSource": "bootstrap",
  "bootstrap": {
    "available": true,
    "provider": "minio",
    "credentialsConfigured": true
  },
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

空 `providers` 表示租户尚未保存自定义配置；`effectiveSource=bootstrap` 时新上传使用部署默认。

`bootstrap` **不得**包含 endpoint、accessKey、secretKey 等敏感字段。

### PUT /admin-api/infra/storage/effective-source

| 项 | 值 |
|----|-----|
| 鉴权 | Bearer JWT |
| 所需权限 | `infra:storage:update` |

**Request body**：

```json
{ "source": "bootstrap" }
```

或 `{ "source": "tenant" }`（须已存在租户 MinIO 配置）。

**Response `data`**：`true`

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
| `isDefault` | **已废弃**（由 `effective-source` 控制） | |

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
{ "source": "bootstrap" }
```

测试部署环境 Bootstrap（密钥不出服务端）。

或租户配置：

```json
{ "source": "tenant", "provider": "minio" }
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

## 历史位置绑定增量（storage-history-binding-hardening）

- 保存草稿不改变新上传目标。Provider 分别保留 `revisionId`（已保存草稿）与 `activeRevisionId`（当前租户上传目标）；即使来源已经是 tenant，也需再次显式启用新草稿。
- `GET config` 增加 `revisions`（id/source/endpoint/bucket/region/pathPrefix/文件与会话引用数/是否历史相对 key；不返回任何 secret）及 `unresolvedFiles`、`unresolvedSessions`。
- 原 `PUT config` 增加 `createNewLocation`；被引用位置改变定位时普通保存拒绝，明确另存新位置才创建新 revision。相同位置更换凭据必须显式轮换。
- `PUT config` 支持 `operation=rotate_credentials`，只接收 `revisionId/accessKey/secretKey`，验证连接后原子替换该位置的加密凭据；不允许带定位修改。影响该位置所有历史引用，失败保留原凭据。
- 维护用途 `operation=register_history` 接收原历史配置、`historySource=bootstrap|tenant`、`legacyRelativeKeys`；只登记待映射位置，不改草稿或当前目标。实际绑定通过内部维护作业默认 dry-run、显式确认计划执行。
- 上传 PUT 前固定位置，confirm HEAD 及文件继承该引用；下载先业务授权，再按文件位置读取，不回退当前默认。新文件完整 objectKey 已含 prefix；旧相对 key 必须通过显式历史位置模式保留原解释。
- `2002001006` 存储位置未解析，历史会话要求重新上传；`2002001007` 同位置凭据变化须显式轮换。引用保护仍 `2002001002`；轮换/测试失败 `2002001003`。权限沿用 infra:storage:query/update/test。
