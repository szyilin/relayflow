# 提案：聊天附件联调（im-message-file-integrate）

## Why

`im-message-file-web` 与 `im-message-file-api` 已完成，须去除附件 Mock，接入真实 Presigned 上传 + 发送 + 下载预览。

## What Changes

- `stores/im.ts` 去 file/image Mock 回退
- 浏览器验证单聊/群聊发图、发文件、点击下载/预览
- 看板 `im-message-file` → **done**

## Capabilities

### Modified Capabilities

- `im`：附件端到端联调完成

## Impact

| 区域 | 影响 |
|------|------|
| `web/` | im store |
| Java | **不改**（除非联调 bugfix） |

## 前置

- `im-message-file-api` archived 或 ready
- `im-message-file-web` ui_ready
