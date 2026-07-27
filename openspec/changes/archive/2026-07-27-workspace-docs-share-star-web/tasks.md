# Tasks：workspace-docs-share-star-web

> 母 change：`workspace-docs-share-star-v1`（Q1–Q7 已拍板）

## 1. Contract 与 API 形状

- [x] 1.1 起草 `openspec/lanes/workspace-docs-share-star/contract.md`
- [x] 1.2 扩展 `web/src/api/app/docs.ts` 类型与 API 函数（可先未接通）

## 2. Store + UI

- [x] 2.1 docs store：分享 / 星标 / visit 临时层 + 列表派生
- [x] 2.2 「最近」多列 + 行菜单；启用 shared/starred 面板
- [x] 2.3 分享对话框；各面板菜单与正文「…」按 D10
- [x] 2.4 看板 web → `in_progress` / `ui_ready`

## 3. 验证

- [x] 3.1 `cd web && pnpm build && pnpm typecheck`
- [x] 3.2 浏览器路径说明：`/app/docs` 最近 / 与我共享 / 星标 / 分享
