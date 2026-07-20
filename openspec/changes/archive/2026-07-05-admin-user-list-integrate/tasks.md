# 任务：admin-user-list-integrate

> 登录、租户、用户列表接真 API；移除 store 层 Mock 回退。

## 后端

- [x] 1.1 `GET /admin-api/system/user/page` 分页（`PageResult`）
- [x] 1.2 MyBatis-Plus 分页插件
- [x] 1.3 `./mvnw -pl relayflow-server -am compile`

## 前端（web/）

- [x] 2.1 `stores/auth.ts` 移除 Mock 登录回退
- [x] 2.2 `stores/tenant.ts` 移除 Mock；失败保留 fallback 文案 + toast
- [x] 2.3 `stores/user.ts` 移除 Mock；失败 toast
- [x] 2.4 `cd web && pnpm build`

## 联调

- [x] 3.1 `spring-boot:run` + `pnpm dev`：登录 → 用户列表显示库内数据 — **关闭（路线重置，不再作为当前 backlog）**
- [x] 3.2 错误密码 / 后端未启动时不再 Mock 假登录 — **关闭（路线重置，不再作为当前 backlog）**

## 门禁

- [x] 3.3 更新看板 admin-shell → done；admin-user-list → done — **关闭（路线重置，不再作为当前 backlog）**
