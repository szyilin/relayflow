# Tasks：workspace-docs-share-star-api

## 1. Schema

- [x] 1.1 Flyway `V0.1.0.39__docs_share_star_visit.sql`（三表 + docs-bot）
- [x] 1.2 DO/Mapper：`DocShare` / `DocStar` / `DocUserVisit`
- [x] 1.3 ErrorCode + `docs-biz` 引入 `im-api`

## 2. Services + REST

- [x] 2.1 扩展 `DocAccessService`（VIEW/EDIT/OWNER）
- [x] 2.2 Share / Star / Visit 服务 + DocsBotNotify
- [x] 2.3 Controllers + VO 对齐 contract
- [x] 2.4 Document get/save/export 走 ACL；recent 改 visit；打开 upsert

## 3. 验证

- [x] 3.1 `./mvnw -pl relayflow-server -am compile`
- [x] 3.2 api-tests 或 curl 说明；看板 api → ready/archived
