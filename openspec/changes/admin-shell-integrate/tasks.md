# 任务：admin-shell-integrate

> **Lane**：集成 · **仅**在 `-web` 代码 tasks 完成且看板 `api: archived` 后执行。

## 前置

- [ ] 0.1 [`api-integration-board.md`](../../../docs/dev/api-integration-board.md) 该切片 `api: archived`
- [ ] 0.2 `admin-shell-web` 全部 tasks 已勾选

## 联调

- [ ] 1.1 `spring-boot:run` + `pnpm dev`：登录后进 `/admin`，navbar 租户名为真 API 数据
- [ ] 1.2 退出登录 → `/admin/login`；清 token 后直访 `/admin` 被拦截
- [ ] 1.3 记录验证账号与路径于本 change `design.md`（或一句引用 login-slice 账号）

## 门禁

- [ ] 2.1 `./mvnw -pl relayflow-server -am compile`
- [ ] 2.2 `cd web && pnpm build`
- [ ] 2.3 `openspec validate admin-shell-web --strict`
- [ ] 2.4 `openspec validate admin-shell-integrate --strict`
- [ ] 2.5 更新看板 `admin-shell` → `web: done`
