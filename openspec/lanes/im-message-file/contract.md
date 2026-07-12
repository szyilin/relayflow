# API 契约：im-message-file

> **状态**：已联调（integrate done）  
> **起草**：`im-message-file-web` change  
> **对接看板**：[`docs/dev/api-integration-board.md`](../../../docs/dev/api-integration-board.md)  
> **基线**：[`im-direct-chat`](../im-direct-chat/contract.md)、[`im-group-chat`](../im-group-chat/contract.md)

## 背景

聊天附件 MVP：用户经 infra Presigned 上传后，以 `file` / `image` content block 发送 IM 消息；消息 API 不代理 multipart 字节。

## 鉴权

| 项 | 值 |
|----|-----|
| REST | Bearer JWT |
| 上传 | 现有 `POST /app-api/infra/file/upload-session|upload-confirm` |
| 下载 | `GET /app-api/infra/file/download?fileId=`（302 至 MinIO） |

## 上传（复用 infra）

聊天附件使用 **`accessLevel=private`**。

```json
POST /app-api/infra/file/upload-session
{
  "filename": "screenshot.png",
  "size": 102400,
  "mimeType": "image/png",
  "accessLevel": "private"
}
```

确认后得到 `fileId`（数字，前端转 string）。

## POST /app-api/im/message/send（扩展）

在文本消息基础上支持 `type=image|file`。

**图片示例**：

```json
{
  "conversationId": "301",
  "clientMsgId": "550e8400-e29b-41d4-a716-446655440000",
  "type": "image",
  "content": {
    "version": 1,
    "blocks": [
      {
        "type": "file",
        "fileId": "1983123456789012345",
        "filename": "screenshot.png",
        "mimeType": "image/png",
        "size": 102400
      }
    ]
  }
}
```

**文件示例**：

```json
{
  "conversationId": "301",
  "clientMsgId": "660e8400-e29b-41d4-a716-446655440001",
  "type": "file",
  "content": {
    "version": 1,
    "blocks": [
      {
        "type": "file",
        "fileId": "1983123456789012346",
        "filename": "report.pdf",
        "mimeType": "application/pdf",
        "size": 204800
      }
    ]
  }
}
```

| 字段 | 说明 |
|------|------|
| `type` | `text`（默认）、`image`、`file` |
| `content.blocks[].type` | 文件块 MUST 为 `file`（image 消息同样用 file block + mimeType） |
| `fileId` | MUST 属于当前租户且上传者为用户本人或同租户有效文件 |

**校验（`-api`）**：

- `type=image`：`mimeType` MUST 以 `image/` 开头
- `type=file`：任意允许 MIME
- 至少一个 `file` block；不得与空 text 混发为无内容消息
- `lastMsgPreview`：`[图片]` / `[文件] filename`

**错误码**：

| code | 说明 |
|------|------|
| `1_003_001_0xx` | 文件不存在或不属于当前租户 |
| `1_003_001_0xx` | content 无效 |

## GET /app-api/im/message/list（响应扩展）

file/image 消息 `content.blocks` 增加 **`downloadUrl`**（可选，由 API 填充）：

```json
{
  "id": "601",
  "type": "image",
  "content": {
    "version": 1,
    "blocks": [
      {
        "type": "file",
        "fileId": "1983123456789012345",
        "filename": "screenshot.png",
        "mimeType": "image/png",
        "size": 102400,
        "downloadUrl": "/app-api/infra/file/download?fileId=1983123456789012345"
      }
    ]
  },
  "seq": 5
}
```

前端 `<img src>` 或下载链接使用 `downloadUrl`（带 JWT cookie/header 由 axios 或 window.open 处理）。

## WebSocket

与文本相同：`message.new` payload 含完整 `content`（含 downloadUrl）。

## curl 示例

```bash
TOKEN="<jwt>"
FILE_ID="1983123456789012345"

curl -s -X POST "http://localhost:8080/app-api/im/message/send" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"conversationId\":301,\"clientMsgId\":\"$(uuidgen)\",\"type\":\"image\",\"content\":{\"version\":1,\"blocks\":[{\"type\":\"file\",\"fileId\":\"$FILE_ID\",\"filename\":\"a.png\",\"mimeType\":\"image/png\",\"size\":1000}]}}"
```

## 前端映射

| UI | Store | API |
|----|-------|-----|
| 附件按钮 | `im.sendFileMessage(file)` | upload-session → send |
| 图片气泡 | message render | list 返回 downloadUrl |
| 文件卡片 | message render | download link |

## V1 不在范围

- 消息内 @提及、引用回复
- 大文件分片（> 单文件上限由 infra 配置）
- 频道专属附件策略
