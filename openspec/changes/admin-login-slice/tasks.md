# 任务：admin-login-slice

> 纵向切片：后端 API 已存在，本 change **以 web/ 为主**。结构见 [`docs/dev/vertical-slice-workflow.md`](../../docs/dev/vertical-slice-workflow.md)。

## 切片：管理端登录

### 后端（仅联调必要项）

- [ ] 1.1 确认 `POST /admin-api/system/auth/login` 与 CORS 在 dev 下可被 `web/` 调用（必要时补 Security/CORS 配置）
- [ ] 1.2 curl 验证登录接口返回 `accessToken` 与 `tenantId`

### 前端（web/）

- [ ] 2.1 新增 `web/src/api/admin/auth.ts`（login）与通用 request 封装（`code === 0`）
- [ ] 2.2 新增 Pinia `useAuthStore`（token、tenantId、login、logout）
- [ ] 2.3 新增 `pages/admin/login.vue`（路由 `/admin/login`）
- [ ] 2.4 路由守卫：未登录访问 `/admin/**`（除 login）→ `/admin/login`；已登录访问 login → `/admin`
- [ ] 2.5 配置 `VITE_API_BASE_URL` 或 dev proxy 指向 `relayflow-server:8080`
- [ ] 2.6 验证：`cd web && pnpm build`

### 联调

- [ ] 3.1 `spring-boot:run` + `pnpm dev`：浏览器完成登录 → 进入 `/admin`
- [ ] 3.2 记录验证账号与路径于 change design（或 README 一句）

### 门禁

- [ ] 4.1 `openspec validate admin-login-slice --strict`
