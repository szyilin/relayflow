# API 契约：infra-file

> **状态**：已冻结（2026-07-11）  
> **起草**：`infra-file-web` change  
> **直传子契约**：[`infra-file-upload/contract.md`](../infra-file-upload/contract.md)  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)

## 背景

管理端文件元数据列表、逻辑删除；上传走 Presigned 直传（⑤）。

## 端点

### GET /admin-api/infra/file/page

| 项 | 值 |
|----|-----|
| 鉴权 | Bearer JWT |
| 所需权限 | `infra:file:list` |

**Query**：

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `pageNo` | int | 1 | 页码 |
| `pageSize` | int | 20 | 每页条数，最大 100 |
| `keyword` | string | — | 模糊匹配 `original_name` |

**Response `data`**：

```json
{
  "list": [
    {
      "id": "2075845810416619521",
      "originalName": "report.pdf",
      "mimeType": "application/pdf",
      "size": 102400,
      "accessLevel": "private",
      "provider": "minio",
      "storageUri": "minio://relayflow/tenant/1/files/2026/07/uuid.pdf",
      "createTime": "2026-07-11T08:00:00Z"
    }
  ],
  "total": 1
}
```

### DELETE /admin-api/infra/file/{id}

| 项 | 值 |
|----|-----|
| 鉴权 | Bearer JWT |
| 所需权限 | `infra:file:delete` |

**Response `data`**：`true`

V1 仅逻辑删除 `infra_file` 行，不删除对象存储中的对象。

## 上传（引用）

| 端点 | 说明 |
|------|------|
| `POST /admin-api/infra/file/upload-session` | 见 [infra-file-upload](../infra-file-upload/contract.md) |
| `POST /admin-api/infra/file/confirm` | 见 [infra-file-upload](../infra-file-upload/contract.md) |

## 前端约定

| 项 | 约定 |
|----|------|
| Composable | `web/src/composables/useDirectUpload.ts` |
| API | `web/src/api/admin/file.ts` |
| Store | `web/src/stores/file.ts` |
| 页面 | `/admin/infra/file` |
| 权限 | `infra:file:list` / `infra:file:upload` / `infra:file:delete` |

## curl 验收

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/admin-api/system/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.data.accessToken')

curl -s "http://localhost:8080/admin-api/infra/file/page?pageNo=1&pageSize=10" \
  -H "Authorization: Bearer $TOKEN" | jq

# 上传见 infra-file-upload contract

curl -s -X DELETE "http://localhost:8080/admin-api/infra/file/{fileId}" \
  -H "Authorization: Bearer $TOKEN" | jq
```
