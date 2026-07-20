## Why

母 change §4：前端去 Mock、接真实 Drive API。§5：跨容器 Library↔Drive 移动一并交付（用户要求同会话）。

## What Changes

- `stores/docsDrive.ts` 改走 `/app-api/docs/drive/**` + infra 上传/下载
- `POST /app-api/docs/drive/placements/move` 实现；contract 固化
- `/app/docs`：文档库「移动到云盘」、云盘「移回文档库」
- E2E / build / typecheck / compile；看板 drive 核心 **done**（含 move）

## Capabilities

### Modified Capabilities

- `docs`: Drive 联调与跨容器 Placement 移动

## Impact

- `web/`、`relayflow-module-docs-biz`、lane contract、对接看板
