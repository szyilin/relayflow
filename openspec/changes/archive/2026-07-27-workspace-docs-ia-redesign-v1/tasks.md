## 1. 规划收口（本母 change）

- [x] 1.1 审阅并锁定产品决策：默认主页/最近；上传=进云盘并拉起上传；文档库树形缩进+窄宽适配；详情检视栏后置并在 design 留底（2026-07-27）
- [x] 1.2 `openspec validate workspace-docs-ia-redesign-v1 --strict` 通过
- [x] 1.3 确认 share-star 基线已合入（`45e7870` · 看板 archived/done；本母 change 不重做 ACL）

## 2. 开子 change（实现入口 · 前端优先）

- [x] 2.1 创建 `workspace-owner-chip-web`（芯片 + 名片弹出；可先于 docs IA）
- [x] 2.2 创建 `workspace-docs-home-ia-web`（主页 Tab + Library 树形列表 + Drive 滑入 UI + 主页上传进云盘 + contract 草案）
- [x] 2.3 创建 `workspace-docs-home-ia-api`（按 contract 补列表 VO / 可选 `UserBasicDTO.avatarFileId`；默认无 Flyway）
- [x] 2.4 创建 `workspace-docs-home-ia-integrate`（去临时数据、联调、任务页芯片收尾）
- [x] 2.5 在 `docs/dev/api-integration-board.md` 登记上述切片状态行

## 3. 归档闸门

- [x] 3.1 子 change 全部完成后：sync 主规格 `openspec/specs/` 反映 Home IA 与 OwnerChip（`docs` / `workspace-owner-chip` / `task`）；**archive 母 change 与子 change 待人工确认后执行**
- [x] 3.2 浏览器路径验收写明：`/app/docs` 默认主页/最近 → 上传进云盘并上传 → 文档库树形列表+返回 → 云盘悬停›滑入树 → 所有者芯片打开名片；`/app/tasks` 负责人芯片
  - 终端联调（2026-07-27）：spring-boot `:8080` + Vite `:5173`；登录 `19988888888`/`123456`；`recent`/`library/tree`/`drive/items`/`starred` 所有者字段与真实昵称对齐；看板 `workspace-docs-ia-redesign` / `workspace-owner-chip` → `web=done`
- [x] 3.3 （后置提醒）IA 归档后另开 `workspace-docs-drive-inspector-*`，以 design「后置提案 · 云盘详情检视栏」为起点展开执行方案
