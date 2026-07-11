# 设计：存储设置管理页 — infra-storage-config-web

## Context

- API 契约：[`infra-storage-config-api`](../infra-storage-config-api/design.md) §D1
- 前端模式：[`admin-ui-patterns.md`](../../../docs/dev/admin-ui-patterns.md) 表单页

## Goals / Non-Goals

**Goals:**

- V1 单 provider（MinIO）配置表单
- 按 `infra:storage:*` 权限显示保存/测试/删除
- 密钥字段不回显；`secretKeyConfigured` 提示留空不修改
- 测试连接：有密钥输入时带表单参数，否则测已保存配置

**Non-Goals:**

- 多 provider 列表 UI（V1 仅 minio）
- Mock 回退（API 已 ready）

## Decisions

### D1：页面结构

```text
/admin/infra/storage
  AdminPageHeader「存储设置」
  UCard（max-w-2xl）
    状态 Badge（active/legacy/default）
    UFormField × endpoint/bucket/accessKey/secretKey/pathPrefix
    UCheckbox useSsl / isDefault
    操作：测试连接 | 保存 | 删除
  UEmpty：无配置时说明可回退 Bootstrap
```

### D2：文件布局

| 路径 | 职责 |
|------|------|
| `api/admin/storage.ts` | HTTP 封装 |
| `stores/storage.ts` | 加载/保存/测试/删除 |
| `pages/admin/infra/storage/index.vue` | 页面 |

### D3：测试连接策略

- `secretKey` 有值 → POST 带完整 inline 参数
- `secretKey` 空且已保存 → POST `{ provider: "minio" }`

## 验证

```bash
cd web && pnpm build
# 浏览器 /admin/infra/storage（super_admin 登录）
```
