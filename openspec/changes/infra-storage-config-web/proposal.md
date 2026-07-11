# 提案：存储设置管理页 — infra-storage-config-web

## Why

③ `infra-storage-config-api` 已提供租户存储配置 CRUD 与测试连接 API，但管理端无页面可操作。须在文件直传切片前补齐 `/admin/infra/storage` UI。

## What Changes

- `openspec/lanes/infra-storage-config/contract.md`（对齐已实现 API）
- `web/src/api/admin/storage.ts` + `stores/storage.ts`
- 页面 `/admin/infra/storage`：MinIO 表单、保存、测试连接、删除
- `useAdminNav` 增加「存储设置」（`infra:storage:query`）
- 更新 API 对接看板

## Capabilities

### New Capabilities

（无新域）

### Modified Capabilities

- `infra`：管理端存储配置 UI 行为

## Impact

| 区域 | 影响 |
|------|------|
| `web/` | 新页面、API、Store、导航 |
| Java | **不改**（API 已在 ③ 完成） |

## 不在本 change

- Upload Session / 文件管理页联调（→ ⑤⑥⑧）
- `-integrate` change（本页直接接真 API，无 Mock）

## 前置

- ③ `infra-storage-config-api` 已实施
