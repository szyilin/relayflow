# 任务：admin-login-slice

> 纵向切片：后端 API 已存在，本 change **将 Mock 替换为真 JWT**（页面/壳层来自 `admin-ui-prototype`，规则来自 `admin-ui-design-direction` 阶段 3）。  
> 前置见 [`docs/dev/admin-ui-workflow.md`](../../docs/dev/admin-ui-workflow.md)。

## 前置（人工 — 阻塞下方所有 tasks）

- [x] 0.1 `admin-ui-prototype` 阶段 8.1：UI 定调签字通过
- [x] 0.2 `admin-ui-design-direction` 阶段 2–3：`admin-ui-tokens.md`、`admin-ui-patterns.md`、`admin-ui-patterns.mdc` 已存在

## 切片：管理端登录（接真 API）

### 后端（仅联调必要项）

- [x] 1.1 确认 `POST /admin-api/system/auth/login` 与 CORS 在 dev 下可被 `web/` 调用（必要时补 Security/CORS 配置）
- [x] 1.2 curl 验证登录接口返回 `accessToken` 与 `tenantId`

### 前端（web/）

- [x] 2.1 新增 `web/src/api/admin/auth.ts`（login）与通用 request 封装（`code === 0`）
- [x] 2.2 新增 Pinia `useAuthStore`（token、tenantId、login、logout）
- [x] 2.3 **保留** `pages/admin/login.vue` 结构与样式；仅改提交逻辑走 `auth.ts` + store
- [x] 2.4 路由守卫：未登录访问 `/admin/**`（除 login）→ `/admin/login`；已登录访问 login → `/admin`
- [x] 2.5 配置 `VITE_API_BASE_URL` 或 dev proxy 指向 `relayflow-server:8080`
- [x] 2.6 验证：`cd web && pnpm build`

### 联调

- [x] 3.1 `spring-boot:run` + `pnpm dev`：浏览器完成登录 → 进入 `/admin`
- [x] 3.2 记录验证账号与路径于 change design（或 README 一句）

### 门禁

- [x] 4.1 `openspec validate admin-login-slice --strict`
