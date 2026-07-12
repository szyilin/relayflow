# 任务：聊天附件 API（im-message-file-api）

> **Lane**：`*-api` · 契约 [`contract.md`](../../lanes/im-message-file/contract.md)

## 1. 前置

- [x] 1.1 `proposal.md` / `design.md` / spec delta / 本 `tasks.md`
- [x] 1.2 阅读 contract 与 `ImMessageServiceImpl`

## 2. infra-api（若需要）

- [x] 2.1 `FileApi.getFile(tenantId, userId, fileId)` 或等价查询（`FileApi.getFile` + 租户隔离）

## 3. im-biz

- [x] 3.1 `ImContentHelper` 扩展 file/image 校验与 preview
- [x] 3.2 `ImMessageServiceImpl` 支持 type=image|file
- [x] 3.3 `listMessages` 填充 block.downloadUrl
- [x] 3.4 `ErrorCodeConstants` + 错误码文档
- [x] 3.5 `SendMessageReqVO.type` 校验

## 4. 验证与看板

- [x] 4.1 curl：upload + send image/file + list 含 downloadUrl
- [x] 4.2 `./mvnw -pl relayflow-server -am compile`
- [x] 4.3 `openspec validate im-message-file-api --strict`
- [x] 4.4 更新 [`api-integration-board.md`](../../../docs/dev/api-integration-board.md) → API `ready`

## 下一 change

- `im-message-file-integrate`
