# Tasks：workspace-docs-drive-web

## 1. Contract

- [x] 1.1 起草 `openspec/lanes/workspace-docs-drive/contract.md`（路径、字段、鉴权、curl 草图）

## 2. UI + Mock store

- [x] 2.1 `/app/docs` 启用「云盘」面板：面包屑、列表、新建文件夹、上传入口；临时数据在 store
- [x] 2.2 FILE 下载按钮形状对齐既有 infra download（`downloadAuthenticatedFile`）
- [x] 2.3 租户切换时 reset Drive 临时状态

## 3. 验证与看板

- [x] 3.1 `cd web && pnpm build && pnpm typecheck`
- [x] 3.2 `openspec validate workspace-docs-drive-web --strict`；母 change §2 勾选；看板 `web → ui_ready`
