# API 契约：admin-shell

> **状态**：已冻结（2026-07-05）  
> **`-web` lane**：只读；字段以本文为准实现 `web/src/api/admin/tenant.ts`  
> **`-api` lane**：确保后端行为与本文一致

## 背景

管理端壳层展示 **默认租户名称**；退出登录为 **前端清 token**（V1 无服务端 logout 接口）。

## 端点

### GET /admin-api/system/tenant/default

| 项 | 值 |
|----|-----|
| 鉴权 | 匿名可访问（Security permitAll） |
| 成功 | `code === 0`，`data` 为租户对象 |

**Response `data` 字段**（与 `TenantRespVO` 一致）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | number | 租户 ID |
| `code` | string | 租户编码 |
| `name` | string | 展示名（navbar 使用） |
| `status` | number | 状态 |
| `createTime` | string (ISO-8601) | 创建时间 |

**curl 示例**：

```bash
curl -s http://localhost:8080/admin-api/system/tenant/default
# 期望：{"code":0,"msg":"成功","data":{"id":1,"name":"默认企业",...}}
```

### GET /admin-api/system/tenant/current（可选，本切片不强制）

| 项 | 值 |
|----|-----|
| 鉴权 | Bearer JWT |
| 用途 | 多租户阶段按 token 内 tenantId 取当前租户；V1 壳层 **优先用 `/default`** |

## 前端对接约定

| 项 | 约定 |
|----|------|
| API 模块 | `web/src/api/admin/tenant.ts` → `getDefaultTenant()` |
| Store | `stores/tenant.ts`：登录后或壳层 mount 时拉取 `name` |
| UI | `AdminNavbar` 展示 `tenantStore.tenantName`；**不改 layout** |
| 退出 | `useAuthStore.logout()` + `router.push('/admin/login')`（已有，本切片仅确认行为） |

## 错误处理

- `code !== 0`：store 保留 fallback 文案「默认企业」或展示 `msg`（toast，不阻断壳层）
- 网络错误：同上，不 crash 壳层

## 非目标（本契约）

- 服务端 logout / token 黑名单
- 租户切换 UI
- 修改 JWT 结构
