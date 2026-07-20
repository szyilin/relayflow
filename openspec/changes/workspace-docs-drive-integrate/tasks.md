# Tasks：workspace-docs-drive-integrate（含 §5 跨容器移动）

## 1. Contract + API move

- [x] 1.1 contract：固化 `POST /placements/move` 字段与错误码
- [x] 1.2 后端实现 move（Library↔Drive；FILE→Library 拒绝）；compile

## 2. 前端联调

- [x] 2.1 `docs-drive` API 客户端 + store 去 Mock；上传/下载/CRUD 走真 API
- [x] 2.2 UI：库「移动到云盘」/ 云盘「移回文档库」；刷新库树与 Drive 列表
- [x] 2.3 租户切换 reset 仍有效

## 3. 验证与看板

- [x] 3.1 curl/smoke：folder + upload + move 往返
- [x] 3.2 `pnpm build` + `pnpm typecheck`；`openspec validate`
- [x] 3.3 母 change §4§5 勾选；看板 drive → **done**
