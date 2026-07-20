## Why

母 change [`workspace-docs-drive-v1`](../workspace-docs-drive-v1/proposal.md) §3：按 [`workspace-docs-drive` contract](../../lanes/workspace-docs-drive/contract.md) 实现「我的文件夹」App API（不含跨容器移动、不含前端联调）。

## What Changes

- `/app-api/docs/drive/**`：文件夹 CRUD、items 列表、登记 FILE、item 改/删
- `docs-biz` 依赖 `infra-api`（`FileApi.getFile` / `bindFile`）
- 错误码：`DOC_DRIVE_FOLDER_NOT_EMPTY` / `DOC_DRIVE_FOLDER_INVALID` / `DOC_STORAGE_FILE_INVALID`
- `FileRespDTO` 暴露 `creator` 供归属校验

## Capabilities

### Modified Capabilities

- `docs`: Drive REST API（仅所有者）

## Impact

- `relayflow-module-docs-*`、`relayflow-module-infra-api` DTO
- 看板 api → `ready`；不改 `web/`
