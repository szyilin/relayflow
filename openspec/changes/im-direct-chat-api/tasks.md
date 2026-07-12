# 任务：单聊 API（im-direct-chat-api）

> **Lane**：`*-api` · 契约真源 [`contract.md`](../../lanes/im-direct-chat/contract.md)

## 1. 前置

- [x] 1.1 阅读 contract、本 change `design.md`、`openspec/specs/im/spec.md` 单聊相关需求

## 2. system-api（跨域昵称）

- [x] 2.1 `UserBasicDTO` + `UserApi.getUserBasic` + `UserApiImpl`

## 3. im-api

- [x] 3.1 `ErrorCodeConstants`；`pom` 引入 `relayflow-common`

## 4. im-biz

- [x] 4.1 `ImConversationService`：列表、direct 去重创建、成员校验
- [x] 4.2 `ImMessageService`：增量列表、发送（seq/幂等/预览）、WS fanout
- [x] 4.3 `controller/app` 三端点 + VO
- [x] 4.4 `ImDomainMessageHandler`（`domain=im`，`message.send`）
- [x] 4.5 `pom`：`infra-api`、`system-api`、`starter-web`、`starter-security`、`validation`

## 5. 验证与看板

- [x] 5.1 `./mvnw -pl relayflow-server -am compile`
- [x] 5.2 `openspec validate im-direct-chat-api --strict`
- [x] 5.3 更新 [`api-integration-board.md`](../../../docs/dev/api-integration-board.md) → API `ready`

## 下一 change

- `im-direct-chat-integrate` — 前端去 Mock + WS 联调
