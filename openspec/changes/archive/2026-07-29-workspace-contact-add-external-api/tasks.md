## 1. 契约与看板

- [x] 1.1 定稿 `openspec/lanes/workspace-contacts-enrich/contract.md` 发起/拒绝章（字段、错误、curl）
- [x] 1.2 看板 `workspace-contact-add-external`：api → in_progress → 完成后 archived

## 2. 领域与 API

- [x] 2.1 `ContactEnrichService`：`searchAccount` / `createRequest` / `rejectRequest`（含 alreadyAdded、查重）
- [x] 2.2 `AppContactController` 暴露三端点；必要时扩展 `ErrorCodeConstants`
- [x] 2.3（可选）Flyway 唯一索引 `pending` 对 `(tenant, from, to)`；无缺口则跳过
  - 应用层查重已足够，本切片不加迁移

## 3. 验证

- [x] 3.1 `./mvnw -pl relayflow-server -am compile`
- [x] 3.2 curl：search-account / create request / reject（及已有 accept 对照）
  - 空搜 `[]`；`13800002222` 命中；create `pending`；重复 `1001002023`；对方 list 可见；reject → `rejected`
- [x] 3.3 `openspec validate workspace-contact-add-external-api --strict`；看板 archived
