# 设计：admin-shell-api

## API 契约

完整契约见 [`openspec/lanes/admin-shell/contract.md`](../../../lanes/admin-shell/contract.md)。

本 change 确保以下端点可用：

```http
GET /admin-api/system/tenant/default
→ 200 { "code": 0, "data": { "id", "code", "name", "status", "createTime" } }
```

实现类：`TenantController#getDefaultTenant` → `TenantService#getDefaultTenant`。

## Security

- `/admin-api/system/tenant/default` 须在 `SecurityAutoConfiguration` 白名单中（匿名可读）
- dev CORS 允许 `http://localhost:5173`（若 `-integrate` 发现跨域问题在此补）

## 验证

```bash
./mvnw -pl relayflow-server -am compile
curl -s http://localhost:8080/admin-api/system/tenant/default | jq .
```

## 与 -web 的边界

| 项 | -api | -web |
|----|------|------|
| TenantController | ✓ | |
| `api/admin/tenant.ts` | | ✓ |
| `stores/tenant.ts` | | ✓ |
| AdminNavbar 展示 | | ✓ |
