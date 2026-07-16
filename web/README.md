# RelayFlow Web

员工工作台（`/app`）与管理后台（`/admin`）的前端，基于 Vue 3 + Nuxt UI v4 + Vite。

## 开发

```bash
pnpm install
pnpm dev
```

默认通过 Vite proxy 转发 `/admin-api`、`/app-api`、`/infra/ws` 到 `http://localhost:8080`。

## 验证

```bash
pnpm build
pnpm typecheck   # 会按需生成 auto-imports.d.ts / components.d.ts
pnpm lint
```

## 约定

- 唯一登录页：`/app/login`（管理端 `/admin/login` 会重定向）
- HTTP：`src/api/request.ts`（axios）；会话键见 `src/utils/session-storage.ts`
- 页面数据优先：Page → Pinia Store → `api/admin|app/*`
- UI：Nuxt UI v4；管理端路由必须以 `/admin` 开头

详见仓库根目录 `docs/dev/code-style.md`、`docs/dev/frontend-first-workflow.md`。
