# 提案：管理端登录纵向切片（admin-login-slice）

## 背景

`tenant-ready-foundation` §4 已提供 `POST /admin-api/system/auth/login` 与 JWT（含 `tenant_id`），但 `web/` 尚无登录页，无法在界面验证。本 change 按 [vertical-slice-workflow.md](../../docs/dev/vertical-slice-workflow.md) **仅补前端与联调**（后端改动限于鉴权/CORS 等必要项）。

## 范围

| 模块 | 内容 |
|------|------|
| `web/` | `/admin/login` 页面、`api/admin/auth.ts`、Pinia auth store、路由守卫、401 跳转 |
| `relayflow-module-system` | 无新业务 API（复用已有登录接口） |
| `relayflow-framework` | 仅当联调需要时调整 CORS / Security 白名单 |

## 非目标

- 用户注册、忘记密码、Refresh Token
- RBAC 菜单权限
- 用户端 `/login`

## 用户可见结果

浏览器访问 `http://localhost:5173/admin/login`（或 dev 端口），输入账号密码后进入 `/admin`，刷新后仍保持登录态。

## 影响域

- OpenSpec：`web-admin`（前端管理端登录）
- Maven：无必须改动（默认不碰 `*-biz`）
