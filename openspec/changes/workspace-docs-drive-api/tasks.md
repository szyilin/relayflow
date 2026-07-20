# Tasks：workspace-docs-drive-api

## 1. 基础

- [x] 1.1 `docs-biz` 增加 `relayflow-module-infra-api`；`ErrorCodeConstants` / `DocConstants` 扩展；`FileRespDTO.creator`
- [x] 1.2 `DocAccessService` 增加 Drive folder/item 所有者校验

## 2. API

- [x] 2.1 文件夹 CRUD + `GET /folders` + `GET /items`（非空删拒绝）
- [x] 2.2 `POST /files`：`FileApi` 校验 + FILE object + drive_item + bindFile
- [x] 2.3 item PUT/DELETE（软删 object+item；不删 infra）

## 3. 验证

- [x] 3.1 `./mvnw -pl relayflow-server -am compile`
- [x] 3.2 curl/smoke：login → folders CRUD → 非空删拒绝 → infra upload + `POST /files` → list → delete item
- [x] 3.3 validate；母 change §3；看板 api → `ready`；更新 contract 状态
