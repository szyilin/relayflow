## Context

`im-message-ux-v1` 已补齐右键菜单、媒体预览、composer 键盘与引用跳转。工作台仍缺飞书式 **表情回应**、**单条转发** 与 **表情包发送**。现有消息模型：`im_message` + `content_json` blocks；实时事件 `message.new` / `message.updated` / `message.recalled`；消息列表 VO 已含 `replyTo` / `recalled`。

约束：Nuxt UI；`/app` 不走管理面 RBAC；文件字节走 `infra_file`；跨域禁止 `*-biz → *-biz`；前端优先纵向切片。

## Goals / Non-Goals

**Goals:**

- 消息级 emoji 回应：增删、聚合展示、右键 / 快捷栏入口、实时同步
- 单条消息转发至本租户其它可写会话
- 系统表情包目录 + sticker 消息类型 + 选择器发送
- app-api + WS 契约；表与 Flyway；工作台联调

**Non-Goals:**

- 多选批量转发、转发到外部租户、邮件/短信转发
- 自定义租户表情包上传 / 管理端编排 UI（表可预留 `scope`，本 change 不实现上传）
- Reaction 自定义图片表情（仅 Unicode emoji）
- 线程回复、消息置顶、已读回执扩展
- Bot / Agent 自动 reaction 或 sticker

## Decisions

### D1 · Reaction 独立表，不写进 content_json

```text
im_message_reaction
  id, tenant_id, conversation_id, message_id, user_id, emoji, create_time, …
  UNIQUE (tenant_id, message_id, user_id, emoji)
```

- 列表 / `message.new` 附带 `reactions: [{ emoji, count, me }]` 聚合；点开详情可另拉用户列表（V1 可先只聚合，悬停显示 count）。
- Toggle：同一用户对同一消息同一 emoji 再点 → 删除。
- 每用户每消息最多 **N=20** 种不同 emoji（防刷）。
- 快捷栏固定 6 个常用（👍 ❤️ 😄 🎉 👀 🙏）+「更多」打开完整选择器。
- 已撤回消息禁止新增 reaction；已有 reaction 仍可展示，不可增改。

**备选（否）**：把 reactions 嵌 `content_json` → 并发写冲突大，且污染消息正文版本。

### D2 · 实时事件

- `domain=im`, `type=reaction.updated`：payload `{ conversationId, messageId, reactions }`（全量聚合快照，客户端替换本地）。
- 不发 `message.updated`，避免与编辑/卡片 patch 语义混淆。

### D3 · 转发 = 目标会话新建消息

- 源消息权限：当前用户须为源会话成员；目标会话须可发消息（成员且未禁言等既有规则）。
- 落库：目标会话新 `im_message`，`seq` 递增；`type` 与内容块 **拷贝**（file 块保留同一 `fileId`，不复制对象存储）；增加块或顶栏元数据：

```json
{
  "forwardedFrom": {
    "conversationId": "...",
    "messageId": "...",
    "senderNickname": "...",
    "preview": "..."
  }
}
```

- `reply_to_msg_id` **不**指向源消息（转发不是引用）；前端用 `forwardedFrom` 渲染「转发自」条。
- 已撤回消息不可转发；card 消息 V1 允许按现有 content 拷贝（只读展示，action 仍绑原消息 id → 目标会话点 action 可能失败，可接受；或 V1 禁止转发 card——**本 change 禁止转发 `type=card`**）。
- 幂等：可选 `client_msg_id`；与普通发送一致。

### D4 · Sticker：新消息 type + 目录表

```text
im_sticker_pack
  id, tenant_id, code, name, sort, status, scope(system|tenant), …

im_sticker
  id, pack_id, tenant_id, code, name, file_id, sort, …
```

- 扩展 `im_message.type` CHECK：加入 `sticker`。
- `content_json` 含一块 `type=sticker`：`{ stickerId, packId, fileId }`；列表填充下载 URL（同 file 块）。
- V1 **只种子系统包**（`scope=system`，资源预置 MinIO 或仓库静态经 infra 登记）；租户包表结构就绪，无管理端上传。
- Composer：表情按钮打开面板（Tab：表情回应常用 / 表情包）；选 sticker → `POST send` type=sticker。

### D5 · 切片顺序（同一 change 内）

1. **-web**：Mock + UI（气泡下 reaction 条、右键「回应/转发」、转发会话选择器、sticker 面板）+ `openspec/lanes/.../contract.md` 草案  
2. **-api**：Flyway + Service + Controller + WS  
3. **-integrate**：去 Mock，接真 API

单 change 交付，tasks 分节；实现会话一次只做一节。

### D6 · API 草形（contract 冻结前可微调）

| Method | Path | 说明 |
|--------|------|------|
| POST | `/app-api/im/message/reaction/toggle` | `{ messageId, emoji }` → 聚合 |
| GET | `/app-api/im/message/reaction/list` | 可选：某消息 reaction 用户明细 |
| POST | `/app-api/im/message/forward` | `{ messageId, targetConversationId, clientMsgId? }` |
| GET | `/app-api/im/sticker/packs` | 当前用户可见包 + stickers |
| POST | `/app-api/im/message/send` | 扩展支持 `type=sticker` + stickerId |

消息列表 `MessageItem` 增：`reactions?`、`forwardedFrom?`。

## Risks / Trade-offs

- [高并发 toggle 聚合不准] → 写库后按 message_id 重算 count；或 DB `COUNT GROUP BY` 读路径
- [转发大文件重复展示] → 共享 `fileId`，不复制 blob；权限仍按原文件租户校验
- [系统 sticker 资源缺失] → 种子脚本登记 file；缺资源时前端占位 + 日志
- [卡片转发语义] → V1 禁止转发 card，菜单项禁用
- [emoji 规范化] → 服务端 NFC 规范化 + 长度上限（≤32 code units）

## Migration Plan

1. Flyway：reaction / pack / sticker 表；`im_message.type` 加 `sticker`
2. 种子系统表情包（可空目录 + 少量占位 sticker）
3. 发布后端与前端；无双写
4. 回滚：回退代码；表可留；type 约束回退需先清 `sticker` 行（开发期 `0.x` 可破坏）

## Open Questions

- 无（产品范围已收入 Goals/Non-Goals；tenant 上传另开 change）
