## Why

母 change `workspace-docs-share-star-v1` 已拍板（Q1–Q7）。本切片按前端优先交付可演示的「与我共享 / 星标 / 最近足迹」UI + contract 草案。

## What Changes

- `/app/docs` 启用「与我共享」「星标」；升级「最近」多列与行菜单
- 分享对话框、星标、离开分享、从最近移除（owner 可删文档）
- Store 临时数据撑演示；起草 `openspec/lanes/workspace-docs-share-star/contract.md`
- 看板 web → `ui_ready`

## Capabilities

### New Capabilities

- （无；行为真源在母 change `docs` / `im` delta）

### Modified Capabilities

- （本 `-web` 不改主 specs；实现范围见母 change）

## Impact

- `web/`：`/app/docs`、docs store、`api/app/docs.ts`
- `openspec/lanes/workspace-docs-share-star/contract.md`
- 看板 `api-integration-board.md`
