# 任务：admin-shell-api

> **Lane**：后端 · 与 `admin-shell-web` 并行；联调见 `admin-shell-integrate`。  
> 契约：[`_lanes/admin-shell/contract.md`](../_lanes/admin-shell/contract.md)

## 前置

- [x] 0.1 `admin-login-slice` 已完成
- [x] 0.2 `_lanes/admin-shell/contract.md` 已冻结

## 后端

- [ ] 1.1 确认 `TenantController` `/default` 返回字段与 contract 一致（`name` 等）
- [ ] 1.2 确认 Security 白名单含 `/admin-api/system/tenant/default`
- [ ] 1.3 若 dev CORS 不足，补 `relayflow-framework` Security/CORS 配置（最小改动）

## 验证

- [ ] 2.1 `./mvnw -pl relayflow-server -am compile`
- [ ] 2.2 `spring-boot:run` 后 curl `/admin-api/system/tenant/default` 返回 `code=0`

## 不在本 change

- 任何 `web/` 修改 → `admin-shell-web`
- 浏览器联调 → `admin-shell-integrate`
