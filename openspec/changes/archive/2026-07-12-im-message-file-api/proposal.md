# 提案：聊天附件 API（im-message-file-api）

## Why

[`im-message-file-web`](../im-message-file-web/proposal.md) 已完成附件 UI 与 [`contract.md`](../../lanes/im-message-file/contract.md)。当前 `ImContentHelper.validateTextContent` 仅接受纯文本，须实现 **file/image 消息校验**、infra 文件引用校验及 list 响应 `downloadUrl`。

## What Changes

- `ImContentHelper`：`validateFileContent()` / 按 type 分支
- `ImMessageService.sendMessage`：支持 `type=image|file`；preview `[图片]`/`[文件]`
- 校验 `fileId` 属于当前租户（经 `infra-api` FileApi，禁止 im-biz 直连 infra 表）
- `listMessages`：为 file block 填充 `downloadUrl`
- `ErrorCodeConstants` 附件相关错误码

## Capabilities

### Modified Capabilities

- `im`：落地 file/image 消息发送与列表 downloadUrl

## Impact

| 区域 | 影响 |
|------|------|
| `relayflow-module-im-biz` | MessageService、ContentHelper、VO |
| `relayflow-module-im-api` | 错误码 |
| `relayflow-module-infra-api` | 依赖 FileApi（若尚无 getFile 则扩展） |
| `web/` | **不改** |
| Flyway | **无** |

## 不在本 change

- 前端去 Mock（`im-message-file-integrate`）
- 已读、在线状态

## 前置

- 契约：[`openspec/lanes/im-message-file/contract.md`](../../lanes/im-message-file/contract.md)
- UI：`im-message-file-web` ui_ready

## 下一 change

- `im-message-file-integrate` — 前端去 Mock + 真实上传联调
