## 1. 联调

- [x] 1.1 移除本切片 store 临时拼装（若有）— 无独立 Mock；列表字段走真实 API
- [x] 1.2 本地 spring-boot + pnpm 联调主路径（浏览器验收）
  - 后端 `dev` profile 已起（`:8080`）；前端 Vite `:5173`（`/app/docs` `/app/tasks` HTTP 200）
  - 登录 `19988888888` / `123456`；烟测：`recent` / `library/tree` / `drive/items` 含 `ownerUserId`+真实 `ownerDisplayName`+`ownerAvatarFileId`；`PUT …/star` 后 `starred` 字段对齐；`shared` 空列表形状 `{items:[]}` OK
  - 浏览器路径：`/app/docs` 默认主页/最近 → Tab 共享/星标 → 文档库树表 → 云盘滑入；所有者芯片；`/app/tasks` 负责人芯片
- [x] 1.3 看板 web=`done`
- [x] 1.4 `openspec validate workspace-docs-home-ia-integrate --strict`
