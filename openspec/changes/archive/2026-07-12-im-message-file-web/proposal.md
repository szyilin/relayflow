# 提案：聊天附件 UI（im-message-file-web）

## Why

`im-group-chat-*` 与 `im-direct-chat-*` 已完成文本消息闭环，但 `/app/messages` 仍无法发送图片/文件。`openspec/specs/im/spec.md` 要求 V1 支持 `file` content block，且 `infra-file` 已有 app 端 Presigned 直传。须按 **前端优先** 完成附件 UI + Mock + 契约，供 `im-message-file-api` 实现。

## What Changes

- `/app/messages` 输入栏：附件按钮（图片/文件选择）+ 消息气泡展示 file/image 块
- 复用 `api/app/file.ts` 的 `uploadPublicFile` 上传流程（private 附件见 design）
- 扩展 `stores/im.ts`：`sendFileMessage()`；Mock 回退
- 起草 `openspec/lanes/im-message-file/contract.md`（send 扩展 + 下载 URL）
- 更新 `docs/dev/api-integration-board.md`

## Capabilities

### Modified Capabilities

- `im`：用户端文件/图片消息 UI 与 REST 契约草案（Java 实现在 `-api`）

## Impact

| 区域 | 影响 |
|------|------|
| `web/` | messages 页、im store/api、mocks |
| Java | **不改** |
| Flyway | **无** |
| 看板 | `im-message-file` web → ui_ready |

## 不在本 change

- `ImContentHelper` / `sendMessage` 文件校验（`im-message-file-api`）
- 去 Mock 联调（`im-message-file-integrate`）
- 已读 UI、在线状态、频道

## 前置

- 单聊/群聊：[`im-direct-chat`](../archive/2026-07-12-im-direct-chat-web/proposal.md)、[`im-group-chat-web`](../im-group-chat-web/proposal.md)
- 文件上传：[`workspace-profile-card`](../../lanes/workspace-profile-card/contract.md) § upload-session

## 下一 change

- `im-message-file-api` — 按 contract 实现 file/image 消息发送与预览 URL
