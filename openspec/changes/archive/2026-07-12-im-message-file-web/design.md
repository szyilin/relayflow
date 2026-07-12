# 设计：聊天附件 UI（im-message-file-web）

## Context

- 架构真源：[`im-platform-foundation` design](../archive/2026-07-12-im-platform-foundation/design.md) Content Block
- UI 真源：[`workspace-ui-patterns.md`](../../../docs/dev/workspace-ui-patterns.md)
- 上传基线：[`web/src/api/app/file.ts`](../../../web/src/api/app/file.ts)（profile 头像已用）

## Goals / Non-Goals

**Goals（MVP）：**

- 输入栏「附件」按钮：选择图片（`image/*`）或任意文件
- 上传成功后发送 `type=image|file` 消息，`content.blocks` 含 `fileId` + 可选 `filename`/`mimeType`
- 消息列表：图片缩略图/预览；文件显示文件名 + 下载链接占位
- 单聊与群聊均可用
- API 未就绪 → store Mock 回退
- `pnpm build` 通过

**Non-Goals：**

- Java 实现、Flyway
- 多文件一次发送、拖拽上传、进度条精细 UX
- 视频、语音消息
- 管理端文件审计

## UI 结构

```text
输入栏
├── 附件按钮（Paperclip）→ hidden <input type="file">
├── 文本框
└── 发送

消息气泡（file/image）
├── image：缩略图 max-h-48，点击新标签预览
└── file：图标 + 文件名 + 大小占位
```

## 数据流

```text
选文件 → uploadPublicFile(file) → fileId
      → POST /app-api/im/message/send { type, content: { blocks: [{ type, fileId, ... }] } }
      → 列表展示；下载 URL 由 contract 约定（302 或 signed URL）
```

V1 聊天附件使用 `accessLevel=private`（见 contract）；头像仍 `public`。

## Mock 策略

| API | 未就绪时 |
|-----|---------|
| `message/send`（file） | Mock 插入本地消息，fileId 用随机字符串 |
| 文件下载 URL | Mock `#` 或 blob URL |

## 验证

```bash
cd web && pnpm build
openspec validate im-message-file-web --strict
```

浏览器（Mock 模式）：

1. `/app/messages` 打开单聊或群聊
2. 点击附件 → 选图片 → 发送 → 气泡展示
3. 选 PDF/文档 → 发送 → 文件卡片展示
