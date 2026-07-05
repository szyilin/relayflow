# 任务：unified-login-slice

## 前端（web/）

- [x] 1.1 `/app/login` 为唯一登录页；成功默认跳转 `/app/messages`
- [x] 1.2 删除双入口：`/`、`/app/login` 不再链到 `/admin/login`
- [x] 1.3 `/admin/login` → 重定向 `/app/login`（保留 query.redirect）
- [x] 1.4 守卫：未登录访问 `/admin/**` → `/app/login`；退出统一回 `/app/login`
- [x] 1.5 `AdminUserMenu` 退出 → `/app/login`
- [x] 1.6 `cd web && pnpm build`

## 文档

- [x] 2.1 更新 `docs/dev/code-style.md`、`frontend-first-workflow.md` 认证约定

## 门禁

- [x] 3.1 `openspec validate unified-login-slice --strict`
