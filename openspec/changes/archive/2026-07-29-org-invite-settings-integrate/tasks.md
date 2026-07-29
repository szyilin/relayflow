## 1. API 客户端与 Store

- [x] 1.1 实现 `api/admin/inviteSettings.ts` 的 `getInviteSettings` / `updateInviteSettings`（`GET`/`PUT /admin-api/system/invite-settings`）
- [x] 1.2 `stores/inviteSettings.ts`：`load`/`save` 只走 API；删除会话内存真源；失败写 `lastError`
- [x] 1.3 更新 `openspec/lanes/org-invite/contract.md`：settings 标注前端已接；去掉「仅 store」残留

## 2. 文档与看板

- [x] 2.1 更新 `docs/dev/admin-ui-patterns.md`：邀请设置数据真源改为 API
- [x] 2.2 更新 `docs/dev/api-integration-board.md`：`org-invite-settings` Web=`done`

## 3. 验证

- [x] 3.1 浏览器：`/admin/system/invite-settings` 保存 → 硬刷新仍可见；可选 curl GET 对照
  - curl：`19988888888`/`123456` 登录后 GET/PUT/GET 持久化 `code=0`；Vite 页 HTTP 200；页面 toast 文案已改为「已写入当前租户」
- [x] 3.2 `cd web && pnpm build && pnpm typecheck`
- [x] 3.3 `openspec validate org-invite-settings-integrate --strict`
