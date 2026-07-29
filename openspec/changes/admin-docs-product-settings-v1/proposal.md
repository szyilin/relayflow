## Why

管理端「产品设置」已为云文档露出占位入口。对照飞书与现状：需补齐 **组织策略（B）**、**企业文件运营（A1）**、**按人存储配额（A2）**；三者与 `/admin/infra/file|storage` 不同，且须分波次交付。

本 change 为母框架 + **已填空的详规**；实现按子 change：`admin-docs-org-policy-*` → `admin-docs-file-ops-*` → `admin-docs-storage-quota-*`。

## What Changes

- `/admin/products/docs` 升级为三页壳层：`permissions` / `files` / `storage`
- **B**：`doc_tenant_settings`（分享/导出/云盘上传开关）+ Admin/App API + 写路径强制
- **A1**：管理员列表/转移所有者/软删/90 天内恢复/销毁（不读正文）
- **A2**：按人 FILE 用量与默认/用户上限；触顶拒上传
- 表前缀 `doc_`；权限码 `docs:*`

## Capabilities

### New Capabilities

- `admin-docs-product-settings`：壳层 + B 策略
- `admin-docs-file-ops`：A1 企业文件运营
- `admin-docs-storage-quota`：A2 按人配额

### Modified Capabilities

- `docs`：生效策略强制；生命周期与 Admin 旁路；上传配额
- `web-admin` / `admin-product-settings`：docs `adminEnabled=true`，去占位

## Impact

| 区域 | 影响 |
|------|------|
| `web/` | `/admin/products/docs/**` |
| `relayflow-module-docs-*` | 设置/文件/配额 Admin API + 强制 |
| Flyway | `doc_tenant_settings`、lifecycle 列、`doc_storage_quota_user_rule` 等 |
| 权限种子 | `docs:product-settings:*`、`docs:files:*`、`docs:storage-quota:*` |

## 非目标

- 外链分享、水印、信息流通、Wiki 组织设置、正文检索、跨产品总存储、管理员读任意正文、用户回收站 UI、部门级配额白名单

## 实现顺序

1. B org-policy（web→api→integrate）
2. A1 file-ops
3. A2 storage-quota
