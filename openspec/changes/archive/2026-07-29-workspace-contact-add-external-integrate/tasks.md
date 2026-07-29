## 1. API 客户端与 Store

- [x] 1.1 `api/app/contacts.ts`：实现 `searchContactAccounts` / `createContactRequest`（对齐 contract；ID 字符串化）
- [x] 1.2 `stores/contactAdd.ts`：`search` / `sendRequest` 只走 API；删除 `buildDraftHits` 与假账号
- [x] 1.3 `stores/contacts.ts`：`rejectRequest` 调 `rejectContactRequest` 并刷新 `fetchRequests`

## 2. 文档与看板

- [x] 2.1 更新 `openspec/lanes/workspace-contacts-enrich/contract.md`：发起/拒绝标注前端已接
- [x] 2.2 更新 `docs/dev/workspace-ui-patterns.md`：添加外部 / 拒绝数据真源改为 API
- [x] 2.3 更新 `docs/dev/api-integration-board.md`：`workspace-contact-add-external` Web=`done`

## 3. 验证

- [x] 3.1 `cd web && pnpm build && pnpm typecheck`
- [x] 3.2 浏览器或 curl：search-account → create request → reject（对照 api 切片）
  - curl：`13800002222` 命中 userId `90001`；create `pending`；重复 `1001002023`；发起人非 to_user reject → `1001002012`（符合仅收件人可拒）
- [x] 3.3 `openspec validate workspace-contact-add-external-integrate --strict`
