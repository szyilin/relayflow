# 提案：管理端登录纵向切片（admin-login-slice）

## 背景

`tenant-ready-foundation` §4 已提供 `POST /admin-api/system/auth/login` 与 JWT（含 `tenant_id`）。本 change 按 [vertical-slice-workflow.md](../../docs/dev/vertical-slice-workflow.md) **将 Mock 登录态替换为真 API**（后端改动限于 CORS 等必要项）。

## 前置条件（文档驱动 — 须全部满足后再实现）

见 [`docs/dev/admin-ui-workflow.md`](../../docs/dev/admin-ui-workflow.md)：

1. **`admin-ui-prototype`** 完成且 **人工签字「UI 定调通过」**
2. **`admin-ui-design-direction`** 阶段 2–3 完成：`docs/dev/admin-ui-tokens.md`、`admin-ui-patterns.md`、`.cursor/rules/admin-ui-patterns.mdc` 已就绪
3. 本 change **保留** 原型中的登录页 layout、样式与壳层结构；**只替换** store / `api/admin/auth.ts` / 路由守卫中的 Mock 逻辑

> 在以上条件满足前，本 change **暂缓实现**。后端 curl 联调可先行，但 **不得** 作为正式 UI 交付重写登录页。

## 范围

| 模块 | 内容 |
|------|------|
| `web/` | `api/admin/auth.ts`、Pinia auth store 接真 API、401 跳转；**不重做** `/admin/login` 视觉 |
| `relayflow-module-system` | 无新业务 API（复用已有登录接口） |
| `relayflow-framework` | 仅当联调需要时调整 CORS / Security 白名单 |

## 非目标

- 用户注册、忘记密码、Refresh Token
- RBAC 菜单权限
- 用户端 `/login`
- 重新定义 UI token 或页面模式（须遵循阶段 3 规则文档）

## 用户可见结果

浏览器访问 `http://localhost:5173/admin/login`（或 dev 端口），输入 **真实** 账号密码后进入 `/admin`，刷新后仍保持登录态；视觉与原型一致。

## 影响域

- OpenSpec：`web-admin`（管理端登录对接）
- Maven：无必须改动（默认不碰 `*-biz`）
