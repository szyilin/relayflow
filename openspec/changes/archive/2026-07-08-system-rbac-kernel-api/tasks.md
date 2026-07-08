# 任务：system-rbac-kernel-api

> **Lane**：后端 · **前置** contract 已冻结。  
> **禁止**改 `web/`（CORS/Security 白名单除外）。

## 前置

- [x] 0.1 阅读 [`openspec/lanes/system-rbac-kernel/contract.md`](../../lanes/system-rbac-kernel/contract.md)
- [x] 0.2 阅读 [`design.md`](design.md)

## 后端

- [x] 1.1 实现 `PermissionService` / `PermissionServiceImpl`（roles + permission codes）
- [x] 1.2 `LoginUser` + `JwtAuthenticationFilter` 填充 authorities
- [x] 1.3 `@EnableMethodSecurity`；`UserController` 加 `@PreAuthorize`；Security 移除 `user/create` permitAll
- [x] 1.4 `GET /admin-api/system/auth/get-permission-info` + VO
- [x] 1.5 `DataScopeHelper` 骨架（本切片不在 user/page 调用）
- [x] 1.6 `./mvnw -pl relayflow-server -am compile` + contract 中 curl 验收

## 归档

- [x] 2.1 更新 [`api-integration-board.md`](../../../docs/dev/api-integration-board.md)：`system-rbac-kernel` → `api: ready`
- [x] 2.2 `openspec validate system-rbac-kernel-api --strict`

## 不在本 change

- `web/` 对接（→ `system-rbac-kernel-web`）
