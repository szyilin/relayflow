# 提案：群聊联调（im-group-chat-integrate）

## Why

`im-group-chat-web` 与 `im-group-chat-api` 已完成，须去除群聊 Mock，接入真实 REST + WS。

## What Changes

- `stores/im.ts` 移除群聊 Mock 回退
- `api/app/im.ts` 群聊响应 ID 归一化

## Impact

| 区域 | 影响 |
|------|------|
| `web/` | im store/api |
| Java | **不改** |
