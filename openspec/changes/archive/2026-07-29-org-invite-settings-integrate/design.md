## Context

前置：`org-invite-settings-web`（UI + 内存 store）与 `org-invite-settings-api`（表 + GET/PUT）均已就绪。本 change 为 **`-integrate`**：前端接真 API、删临时真源、看板 done、浏览器走查。

契约真源：[`openspec/lanes/org-invite/contract.md`](../../lanes/org-invite/contract.md) settings 章。

## Goals / Non-Goals

**Goals:**

- `getInviteSettings` / `updateInviteSettings` 调用 `/admin-api/system/invite-settings`
- store `load`/`save` 以 API 为唯一真源；失败可见 `lastError` + toast
- 租户切换已有 `resetForTenantSwitch` 保持清空后下次 `load`
- 看板 `web → done`；`pnpm build` + `pnpm typecheck`；浏览器验收刷新持久化

**Non-Goals:**

- 不改后端表结构 / Controller（缺口另开 `-api` 修补）
- 不接工作台四渠道 / capability（`org-invite-channels-integrate`）
- 不改页面 template 布局（除非字段映射缺口）
- 不新增独立 permission 码

## Decisions

### D1. API 客户端

在既有 `web/src/api/admin/inviteSettings.ts` 用 `get` / `put` 实现；路径与类型已与 contract / VO 对齐（camelCase）。

### D2. Store 行为

- `load()`：`GET` → 写入 `settings`；失败设 `lastError` 并 throw（页面 toast）
- `save(next)`：前端校验指定部门非空（与 `-web` 一致）→ `PUT` 全量 body → 用响应 `data` 回写 store
- 删除「未 hydrated 则塞 DEFAULT」作为真源的路径；默认仅作类型兜底 / 表单初始，以 API 返回为准

### D3. 联调与验收

```text
1. docker compose up -d（若需）
2. ./mvnw -pl relayflow-server -am spring-boot:run
3. cd web && pnpm dev
4. /app/login → 管理身份进 /admin/system/invite-settings
5. 改模式并保存 → 硬刷新 → 值仍在
6. curl GET 对照页面
```

### D4. 看板与文档

- 看板：`org-invite-settings` → Web **done**
- `admin-ui-patterns`：数据真源改为 API
- contract：settings 标注 integrate 完成 / 前端已接

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| API 未起或迁移未跑 | tasks 写明先起 server；失败可见错误 |
| 字段名漂移 | 以 contract / RespVO 为准 |
| 无权限 403 | 与现有 `request.ts` 行为一致；页面已按 permission 控按钮 |

## Migration Plan

1. 部署含 invite-settings API 的后端并跑 Flyway
2. 发布前端 integrate
3. 回滚：回退前端提交；DB 行保留无害

## Open Questions

- （无）
