# 提案：workspace-docs-library-integrate（联调）

## Why

母 change [`workspace-docs-library-v1`](../workspace-docs-library-v1/proposal.md) §4：`-web` UI 与 `-api` 后端已就绪，需将 `web/src/stores/docs.ts` 接入真实 `/app-api/docs/**`，去除 store 内临时 Map 与客户端 MD 导出。

## What Changes

- Store 调用 `web/src/api/app/docs.ts`（树 / 文档 / 最近 / 导出）
- 错误码映射（尤其 `DOC_VERSION_CONFLICT`）
- 看板 `workspace-docs-library` web → **done**；母 change §4 勾选

## Capabilities

### Modified Capabilities

- `docs`：我的文档库前端 store 真源为 API

## Impact

`web/src/stores/docs.ts`、可选 `useTenantSwitchReload` 补拉、`docs/dev/api-integration-board.md`、母 change §4–§5.2
