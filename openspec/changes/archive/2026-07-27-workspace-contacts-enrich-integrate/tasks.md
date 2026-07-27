## 1. 前置与 API 客户端

- [x] 1.1 确认 `workspace-contacts-enrich-api` 已可用（迁移已跑、curl list/accept/star 通）
- [x] 1.2 在 `web/src/api/app/` 增加 enrich 客户端（list ×3、accept、star PUT），类型对齐 contract
- [x] 1.3 更新 `openspec/lanes/workspace-contacts-enrich/contract.md` 前端映射表（去掉「临时数组」）

## 2. Store 去 Mock

- [x] 2.1 `stores/contacts.ts`：删除 `seedExternal` / `seedRequests` / `seedStarred` 及初始化假数据
- [x] 2.2 按 category 拉取外部 / 申请 / 星标；loading / error 可见
- [x] 2.3 `acceptRequest` / 星标 toggle 改为调 API 并刷新相关列表
- [x] 2.4 租户 reset 清空 enrich 状态；「我的群组」无临时群真源

## 3. 文档与验收

- [x] 3.1 更新 `docs/dev/workspace-ui-patterns.md` 通讯录：数据真源改为 API（无本地同意/假数据）
- [x] 3.2 更新 `docs/dev/api-integration-board.md`：`workspace-contacts-enrich` Web=`done`；「建议下一切片」去掉本项待办
- [x] 3.3 浏览器：`/app/contacts` → 外部 / 新的联系人（同意）/ 星标（取消）/ 我的群组；刷新后状态保持
  - Vite `:5173` `/app/contacts` HTTP 200；后端 `:8080`；登录后 API：external list / request list / accept / star / unstar 均 `code=0`；页面走 store → 同端点
- [x] 3.4 `cd web && pnpm build && pnpm typecheck`
- [x] 3.5 `openspec validate workspace-contacts-enrich-integrate --strict`
