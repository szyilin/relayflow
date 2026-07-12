# 设计：聊天附件 API（im-message-file-api）

## Context

- 契约：[`contract.md`](../../lanes/im-message-file/contract.md)
- 单聊实现：`ImMessageServiceImpl`、`ImContentHelper`
- Infra 文件：[`infra-file`](../../lanes/infra-file/contract.md)

## Goals / Non-Goals

**Goals：**

- `sendMessage` 接受 `type=image|file` + file content block
- 校验 fileId 存在、tenant 一致、上传会话已完成
- `buildPreview`：`[图片]` 或 `[文件] {filename}`
- `listMessages` 为每个 file block 设置 `downloadUrl=/app-api/infra/file/download?fileId=`
- `./mvnw compile` + curl 通过

**Non-Goals：**

- 新 Flyway 表
- IM 代理文件字节
- 病毒扫描、MIME 嗅探

## 实现要点

```text
ImContentHelper
├── validateUserMessage(type, content)
│   ├── text → 现有逻辑
│   └── image|file → 至少一个 file block，fileId 非空
└── buildPreview → [图片] / [文件] name

ImMessageServiceImpl.sendMessage
└── resolveMessageType(request.getType()) → text|image|file|system

File 校验（im-biz → infra-api）
└── FileApi.getFile(tenantId, fileId) 或 validateAccessible(userId, fileId)
```

## 验证

```bash
./mvnw -pl relayflow-server -am compile
# 先 upload-session + confirm 得 fileId，再 curl send
openspec validate im-message-file-api --strict
```
