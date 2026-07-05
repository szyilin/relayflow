# API 契约：system-rbac-kernel

> **状态**：已冻结（2026-07-05）  
> **起草**：`system-rbac-kernel-api` change  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)

## 背景

管理端 RBAC 运行时：登录用户加载 `permission.code`，API 按权限拦截，前端按同一套 codes 控制菜单。

鉴权链路：`user → sys_user_role → sys_role → sys_role_permission → sys_permission.code`

## 端点

### GET /admin-api/system/auth/get-permission-info

| 项 | 值 |
|----|-----|
| 鉴权 | Bearer JWT（必须已登录） |
| 所需权限 | 无（任意已登录租户成员可拉取 **自己的** 权限） |

**Response `data`**：

```json
{
  "userId": 1,
  "username": "admin",
  "nickname": "管理员",
  "roles": [
    {
      "id": 100,
      "code": "super_admin",
      "name": "超级管理员"
    }
  ],
  "permissions": [
    "system",
    "system:user",
    "system:user:list",
    "system:dept:list",
    "system:role:list"
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | number | `sys_user.id` |
| `username` | string | 登录名 |
| `nickname` | string | 昵称；空则回退 username |
| `roles` | array | 当前租户下用户绑定的角色摘要 |
| `roles[].id` | number | `sys_role.id` |
| `roles[].code` | string | 可空（自定义角色） |
| `roles[].name` | string | 角色名 |
| `permissions` | string[] | 去重后的 `sys_permission.code` 列表 |

**curl**：

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/admin-api/system/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.data.accessToken')

curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/admin-api/system/auth/get-permission-info | jq
```

### 权限拦截（现有端点回归）

| 端点 | 所需 authority |
|------|----------------|
| `GET /admin-api/system/user/page` | `system:user:list` |
| `POST /admin-api/system/user/create` | `system:user:create` |

无权限时：**HTTP 403**（Spring Security），body 遵循全局异常处理（若已有 403 JSON 则沿用）。

仍 **permitAll** 的端点（本切片不变）：

- `POST /admin-api/system/auth/login`
- `GET /admin-api/system/tenant/default`

## 前端约定

| 项 | 约定 |
|----|------|
| API | `web/src/api/admin/auth.ts` → `getPermissionInfo()` |
| Store | `stores/auth.ts` → `permissions: string[]`、`fetchPermissionInfo()` |
| Composable | `composables/usePermission.ts` → `hasPermission(code)` |
| Nav | `useAdminNav.ts` 每项可选 `permission?: string` |
| 调用时机 | 登录成功后 + 已有 token 进入 `/admin` 壳层时 |

### Nav permission 映射（V1 静态）

| 菜单 | permission |
|------|------------|
| 用户管理 | `system:user:list` |
| 部门管理 | `system:dept:list` |
| 角色管理 | `system:role:list`（**`admin-role-slice` 再加 nav**；本 kernel-web 仅过滤已有项） |
| 文件管理 | 暂不设（infra 未接 RBAC） |
| 概览 | 不设（登录即可见） |

## 数据范围（本切片仅后端框架）

`DataScopeHelper` 供后续 `admin-user-mutate-slice` 使用；本切片 **不要求** 改 user/page 过滤逻辑。

多角色合并规则：**权限 codes 取并集**；data scope deptIds 取并集（任一角色 `ALL` → ALL）。
