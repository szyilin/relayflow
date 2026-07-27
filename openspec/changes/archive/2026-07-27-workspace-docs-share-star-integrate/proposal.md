## Why

`-web` 与 `-api` 已就绪；本切片去掉 store 临时协作层，联调真实 share / shared / star / recent API。

## What Changes

- `useDocsStore` 删除 `USE_COLLAB_TEMP` 与本地 seed；shared / starred / recent / shares 只走 `/app-api/docs/**`
- `/app/docs`：分享对话框、星标、离开分享、从最近移除、VIEW 只读编辑器走真 API
- 看板 `workspace-docs-share-star` → **done**；母 change §3 勾选

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `docs`: store 联调真源（分享 / 星标 / 最近足迹），不再使用客户端临时 overlay

## Impact

- `web/src/stores/docs.ts`、`web/src/pages/app/docs/index.vue`
- 对接看板、母 change `workspace-docs-share-star-v1` §3
- 无 Flyway / 后端改动；回滚仅前端回退
