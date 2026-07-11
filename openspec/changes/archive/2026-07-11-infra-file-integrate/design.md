# 设计：infra-file-integrate

## 验证路径

1. `./mvnw -pl relayflow-server -am compile`
2. `cd web && pnpm build`
3. `/admin/infra/file`：上传 → 列表 → 下载 → 删除
4. public 文件 curl `-I` `/app-api/infra/file/public/{id}`

## Archive 范围

`infra-storage-config-api`、`infra-storage-config-web`、`infra-file-upload-api`、`infra-file-web`、`infra-file-download-api`
