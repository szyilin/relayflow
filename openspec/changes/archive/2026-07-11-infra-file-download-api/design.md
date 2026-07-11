# 设计：文件下载分流 — infra-file-download-api

## Decisions

### D1：端点

| access | Method | Path | 鉴权 |
|--------|--------|------|------|
| public | GET | `/app-api/infra/file/public/{fileId}` | 无（permitAll） |
| private | GET | `/admin-api/infra/file/{fileId}/download` | JWT + `infra:file:download` |

### D2：行为

1. 加载 `infra_file`（租户上下文；V1 默认 tenant=1）
2. public 端点拒绝 `access_level != public`
3. `resolveProviderConfig(file.provider)` → presigned GET
4. HTTP 302 `Location: {presignedUrl}`
5. Cache-Control：public `max-age=31536000`；private 不设置长缓存（presigned TTL 15min）

### D3：错误码

`2_002_002_008` — 文件访问级别不允许

## 验证

curl `-I` 检查 302 与 Cache-Control
