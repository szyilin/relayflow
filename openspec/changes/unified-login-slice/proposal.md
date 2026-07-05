# 提案：统一登录入口（unified-login-slice）

## 背景

当前存在 `/app/login` 与 `/admin/login` 两个登录页，登录后分别进入工作台与管理端。产品约定：**同一账号体系**，普通用户与管理员由 **权限** 区分，而非不同登录入口。

`/app/login` 已通过 `admin-login-slice` 对接 `POST /admin-api/system/auth/login`。

## 范围

| 模块 | 内容 |
|------|------|
| `web/` | 保留 `/app/login` 为唯一登录页；登录成功 → `/app/messages`；`/admin/login` 重定向；守卫、退出、首页入口统一 |
| 后端 | 无改动 |

## 非目标

- RBAC 菜单权限（后续切片）
- 隐藏工作台「管理后台」入口（后续按权限显示）

## 用户可见结果

- 访问 `/` → 引导至 `/app/login`
- 登录 → 工作台
- 未登录访问 `/admin/**` → `/app/login?redirect=...`
- 工作台侧栏可进管理后台（有 token 即可；403 门禁后续做）
