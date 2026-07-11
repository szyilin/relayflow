# API 契约：infra-file-upload

> **状态**：已冻结（2026-07-11）  
> **起草**：`infra-file-upload-api` change  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)

## 背景

管理端 Presigned 直传三阶段：创建会话 → 浏览器 PUT → 确认落库 `infra_file`。V1 仅 MinIO `presigned_put` 模式。

## 端点

### POST /admin-api/infra/file/upload-session

| 项 | 值 |
|----|-----|
| 鉴权 | Bearer JWT |
| 所需权限 | `infra:file:upload` |

**Request body**：

```json
{
  "filename": "report.pdf",
  "size": 102400,
  "mimeType": "application/pdf",
  "accessLevel": "private"
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| `filename` | 是 | 原始文件名（用于提取扩展名） |
| `size` | 是 | 字节数，> 0 |
| `mimeType` | 是 | Content-Type |
| `accessLevel` | 否 | `public` \| `private`，默认 `private` |

**Response `data`**：

```json
{
  "uploadId": "1983123456789012345",
  "mode": "presigned_put",
  "objectKey": "tenant/1/files/2026/07/550e8400-e29b-41d4-a716-446655440000.pdf",
  "uploadUrl": "http://127.0.0.1:9000/relayflow/tenant/1/files/...",
  "headers": { "Content-Type": "application/pdf" },
  "expiresAt": "2026-07-11T08:00:00Z"
}
```

### POST /admin-api/infra/file/confirm

| 项 | 值 |
|----|-----|
| 鉴权 | Bearer JWT |
| 所需权限 | `infra:file:upload` |

**Request body**：

```json
{
  "uploadId": "1983123456789012345",
  "etag": "\"d41d8cd98f00b204e9800998ecf8427e\"",
  "size": 102400
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| `uploadId` | 是 | 会话 ID |
| `size` | 是 | 须与对象实际大小一致 |
| `etag` | 否 | 浏览器 PUT 响应 ETag；非空时服务端比对 |

**Response `data`**：

```json
{
  "fileId": "1983123456789012346",
  "storageUri": "minio://relayflow/tenant/1/files/2026/07/550e8400-e29b-41d4-a716-446655440000.pdf"
}
```

## 错误码

| code | 含义 |
|------|------|
| `2002002001` | 上传会话不存在 |
| `2002002002` | 上传会话已过期 |
| `2002002003` | 上传会话状态无效 |
| `2002002004` | 对象存储中未找到已上传对象 |
| `2002002005` | 确认时文件大小不匹配 |
| `2002002007` | 上传请求参数无效 |

## curl 验收（直传 MinIO）

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/admin-api/system/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.data.accessToken')

SESSION=$(curl -s -X POST http://localhost:8080/admin-api/infra/file/upload-session \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"filename":"test.txt","size":13,"mimeType":"text/plain","accessLevel":"private"}')
echo "$SESSION" | jq

UPLOAD_URL=$(echo "$SESSION" | jq -r '.data.uploadUrl')
UPLOAD_ID=$(echo "$SESSION" | jq -r '.data.uploadId')
CONTENT_TYPE=$(echo "$SESSION" | jq -r '.data.headers["Content-Type"]')

echo -n 'hello relayflow' > /tmp/rf-upload-test.txt
ETAG=$(curl -s -D - -o /dev/null -X PUT "$UPLOAD_URL" \
  -H "Content-Type: $CONTENT_TYPE" --data-binary @/tmp/rf-upload-test.txt | \
  tr -d '\r' | awk '/^etag:/ {print $2; exit}')

curl -s -X POST http://localhost:8080/admin-api/infra/file/confirm \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{\"uploadId\":\"$UPLOAD_ID\",\"etag\":\"$ETAG\",\"size\":13}" | jq
```

## 前端约定（⑥ 对接）

| 项 | 约定 |
|----|------|
| Composable | `web/src/composables/useDirectUpload.ts` |
| API | `web/src/api/admin/file.ts` |
| 页面 | `/admin/infra/file` |
| 权限 | `infra:file:upload` |

**禁止**页面直接 import MinIO SDK 或持有 AK/SK。
