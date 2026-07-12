# 任务：聊天附件 UI（im-message-file-web）

> **Lane**：`*-web` 第一步。后端见 `im-message-file-api`。

## 1. 前置

- [x] 1.1 `proposal.md` / `design.md` / spec delta / 本 `tasks.md`
- [x] 1.2 阅读 [`contract.md`](../../lanes/im-message-file/contract.md) 与 `design.md`

## 2. 前端（web/）

- [x] 2.1 起草 `openspec/lanes/im-message-file/contract.md`
- [x] 2.2 `api/app/file.ts`：增加 `uploadPrivateFile()`（`accessLevel=private`）
- [x] 2.3 `api/app/im.ts`：扩展 `sendMessage` 支持 `type=image|file` 与 file block
- [x] 2.4 `mocks/im.ts`：file/image 消息 Mock 数据
- [x] 2.5 `stores/im.ts`：`sendFileMessage()` + Mock 回退
- [x] 2.6 `/app/messages`：附件按钮、file/image 气泡组件
- [x] 2.7 更新 [`api-integration-board.md`](../../../docs/dev/api-integration-board.md)

## 3. 验证

- [x] 3.1 `cd web && pnpm build`
- [x] 3.2 `openspec validate im-message-file-web --strict`

## 浏览器路径

1. `pnpm dev` → `/app/login` 登录
2. `/app/messages` → 单聊或群聊 → 附件 → 选图/文件 → 发送
3. API 未实现时 Mock 仍展示气泡；上传可走真实 infra

## 下一 change

- `im-message-file-api` — file/image 消息校验 + infra file 引用校验 + 下载 URL
