# 设计：管理端登录切片

## 技术选型

| 层 | 选型 |
|----|------|
| API 封装 | `web/src/api/admin/auth.ts`，axios/fetch 统一 `{ code, msg, data }` |
| 状态 | Pinia `useAuthStore`：`accessToken`、`tenantId` |
| 存储 | `localStorage`（V1）；key 前缀避免与其他端冲突 |
| 路由 | 文件路由 `pages/admin/login.vue`；守卫：无 token 访问 `/admin/*` → `/admin/login` |
| UI | Nuxt UI：`UAuthForm` 或 `UCard` + `UInput` + `UButton` |

## 联调与演示

```text
1. docker compose -f deploy/compose.yml up -d   # PostgreSQL
2. ./mvnw -pl relayflow-server -am spring-boot:run
3. 若无用户：curl POST /admin-api/system/user/create（bootstrap）
4. cd web && pnpm dev
5. 打开 http://localhost:5173/admin/login
6. 登录 admin / admin123 → 跳转 /admin
7. 刷新页面仍 authenticated；清除 token → 重定向登录
```

环境变量：`VITE_API_BASE_URL=http://localhost:8080`（或 Vite proxy）。

## 安全注意

- 不在前端硬编码 JWT secret
- 登录失败展示统一 `msg`，不区分用户是否存在（与后端一致）
