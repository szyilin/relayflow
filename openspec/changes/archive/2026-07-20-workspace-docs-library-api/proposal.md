# 提案：workspace-docs-library-api（[后端]）

## Why

母 change [`workspace-docs-library-v1`](../workspace-docs-library-v1/proposal.md) §3：`-web` contract 已冻结，需实现 `/app-api/docs/**` 供 integrate 替换 store 临时数据。

## What Changes

- 按 [`contract.md`](../../lanes/workspace-docs-library/contract.md) 实现 Library 树 CRUD、文档读写、最近、MD 导出
- `ErrorCodeConstants`（DOC_*）；看板 api → `ready`
- **不改** `web/`（integrate 另做）

## Capabilities

### Modified Capabilities

- `docs`：我的文档库 App API（所有者 `owner_user_id`）

## Impact

`relayflow-module-docs-*`、contract 错误码、看板、母 change §3
