# Specs 归档前 diff 检查（§8.4）

归档 `multi-tenant-account-v2` 时，`openspec archive` 将把下列 delta 合并进 `openspec/specs/system/spec.md`：

| 区域 | 变更摘要 |
|------|----------|
| 开放注册 | 新增 `POST /app-api/system/auth/register` 行为 |
| 多企业登录 | `TENANT_SELECTION_REQUIRED`、`tenantId` 登录参数 |
| 企业切换 | `GET/POST /app-api/system/tenant/my-list|switch` |
| 管理端邀请 | `enabled=true` 时 tenant 来自 JWT |
| **REMOVED** | `member-invite/preview|accept` 独立接受流程 → 注册合并 |

**检查命令**（归档前人工执行）：

```bash
openspec validate multi-tenant-account-v2 --strict
diff -u openspec/specs/system/spec.md openspec/changes/multi-tenant-account-v2/specs/system/spec.md | head -80
```

确认 REMOVED 段与 design §D5 一致后再 `openspec archive multi-tenant-account-v2`。
