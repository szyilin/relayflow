# 任务：infra-file-web

> **Lane**：前端 · 上传 API（⑤）已 ready；列表/删除 API 随 contract 同期实现。

## 契约

- [x] 1.1 起草 `openspec/lanes/infra-file/contract.md`（列表 + 删除 + 引用 upload）

## 后端（列表/删除，contract 同期）

- [x] 2.1 `GET /admin-api/infra/file/page` + `DELETE /admin-api/infra/file/{id}`
- [x] 2.2 `./mvnw -pl relayflow-server -am compile`

## 前端

- [x] 3.1 `composables/useDirectUpload.ts`
- [x] 3.2 `api/admin/file.ts` + `stores/file.ts`
- [x] 3.3 改造 `/admin/infra/file`（列表 + 上传 + 删除）
- [x] 3.4 `useAdminNav` 文件管理增加 `infra:file:list`

## 验证

- [x] 4.1 `cd web && pnpm build`
- [x] 4.2 更新 `docs/dev/api-integration-board.md`
- [x] 4.3 `openspec validate infra-file-web --strict`

## 不在本 change

- 下载 API / 公开访问（→ `infra-file-download-api`）
- integrate 去 Mock（→ `infra-file-integrate`）
