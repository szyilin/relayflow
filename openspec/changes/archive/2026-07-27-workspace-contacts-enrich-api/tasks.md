## 1. 契约与表

- [x] 1.1 对照并冻结 `openspec/lanes/workspace-contacts-enrich/contract.md`（路径/字段/鉴权与本 design 一致；状态改为实现中或 ready）
- [x] 1.2 Flyway `V0.1.0.40__sys_contact_enrich.sql`（若 40 已被占用则顺延 seq）：创建 `sys_contact_external`、`sys_contact_request`、`sys_contact_star` 及唯一/查询索引
- [x] 1.3 `./scripts/codegen.sh --module system --tables sys_contact_external,sys_contact_request,sys_contact_star` → diff 合入 DO/Mapper；自定义查询用 ExtMapper

## 2. 服务与 API

- [x] 2.1 Service：外部列表组装（displayName / orgLabel / avatarText / sortLetter）+ 排序
- [x] 2.2 Service：申请列表；`accept` 校验归属与 pending → accepted + upsert external
- [x] 2.3 Service：星标 list + PUT upsert/取消
- [x] 2.4 `AppContactController`（或等价）暴露 contract 五类端点；统一 `code=0` 响应

## 3. 验证与看板

- [x] 3.1 文档化验收用 SQL（插入 pending 申请 / 外部 / 星标样例，不强制迁入 Flyway seed）
  - 见同目录 `verify-seed.sql`
- [x] 3.2 curl 走通 list ×3 + accept + star PUT（见 contract）
  - 登录 `POST /admin-api/system/auth/login`（`19988888888`/`123456`）；external/request/starred list OK；accept pending→accepted；重复 accept 返回 `CONTACT_REQUEST_INVALID`；star/unstar OK
- [x] 3.3 `./mvnw -pl relayflow-server -am compile`
- [x] 3.4 更新 `docs/dev/api-integration-board.md`：本切片 `API → ready`（Web 仍 `ui_ready`）
- [x] 3.5 `openspec validate workspace-contacts-enrich-api --strict`
