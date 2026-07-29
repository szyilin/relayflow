## 1. API 客户端与 Store

- [x] 1.1 新增 `api/app/orgInvite.ts`：`getCapability` / `getSharedInvite` / `resetSharedInvite` / `createMobileInvite` / `consumeInvite`（对齐 contract）
- [x] 1.2 `stores/orgInvite.ts`：ensure/reset/mobile/consume 只走 API；删除随机 token 与本地校验真源；失败写 `lastError`
- [x] 1.3 `stores/createHub.ts`：`canInvite` 改读 capability（经 orgInvite）；进入壳/`+` 前 loadCapability
- [x] 1.4 更新 Modal / `/app/invite/join` 调用与成功/失败文案（不重做布局）

## 2. 文档与看板

- [x] 2.1 更新 `openspec/lanes/org-invite/contract.md`：channels 标注前端已接；去掉「仅草稿」残留
- [x] 2.2 更新 `docs/dev/workspace-ui-patterns.md`：四渠道 / 加入页数据真源改为 API
- [x] 2.3 更新 `docs/dev/api-integration-board.md`：`org-invite-channels` Web=`done`

## 3. 验证

- [x] 3.1 浏览器：`+` → 添加企业成员（ensure/重置）；`/app/invite/join` 消费成功；可选 curl 对照
  - curl：`19988888888`/`123456` 登录后 capability/shared/reset/consume/mobile 均 `code=0`；旧 token consume 返回失效；已是成员 `alreadyMember=true`
- [x] 3.2 `cd web && pnpm build && pnpm typecheck`
- [x] 3.3 `openspec validate org-invite-channels-integrate --strict`
