# 任务：聊天附件联调（im-message-file-integrate）

> **Lane**：`*-integrate` · 契约 [`contract.md`](../../lanes/im-message-file/contract.md)

## 1. 前端联调

- [x] 1.1 `stores/im.ts` 去 file/image Mock 回退
- [x] 1.2 下载/预览 URL 携带 JWT（`ImAuthenticatedImage` + `fetchAuthenticatedBlobUrl`）
- [x] 1.3 单聊 + 群聊冒烟

## 2. 验证与看板

- [x] 2.1 `./mvnw -pl relayflow-server -am compile`
- [x] 2.2 `cd web && pnpm build`
- [x] 2.3 更新 [`api-integration-board.md`](../../../docs/dev/api-integration-board.md) → **done**
- [x] 2.4 `openspec validate im-message-file-integrate --strict`

## 浏览器路径

1. `/app/messages` 单聊 → 附件发图 → 双方可见
2. 群聊 → 附件发 PDF → 下载成功
