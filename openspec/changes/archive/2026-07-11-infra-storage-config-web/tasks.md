# 任务：infra-storage-config-web

> **Lane**：前端 · API 已在 ③ ready，本 change 直接接真 API。

## 契约与 API 层

- [x] 1.1 起草 `openspec/lanes/infra-storage-config/contract.md`
- [x] 1.2 `web/src/api/admin/storage.ts` + `stores/storage.ts`

## 页面

- [x] 2.1 页面 `/admin/infra/storage`（MinIO 表单 + 测试连接 + 保存/删除）
- [x] 2.2 `useAdminNav` 增加「存储设置」（`infra:storage:query`）

## 验证与看板

- [x] 3.1 更新 `docs/dev/api-integration-board.md`
- [x] 3.2 `cd web && pnpm build`；浏览器 `/admin/infra/storage`
- [x] 3.3 `openspec validate infra-storage-config-web --strict`
