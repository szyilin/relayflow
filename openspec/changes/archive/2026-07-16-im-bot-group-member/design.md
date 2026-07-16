## Context

- 母 change `im-bot-notify-foundation` 已完成 G0：`im_conversation_member.subject_type`、Bot 目录、`bot_dm` Outbound。
- 现网 `ImGroupService` 仅按 `userId` 加人，`listMembers` 过滤 `subject_type=USER`。
- 架构分期：G1 挂载 → G2 @ → G3 Runtime；Outbound 不依赖本切片。

## Goals / Non-Goals

**Goals:**

1. 授权成员可将已启用/系统 Bot 加入群，并可移除
2. 成员列表与前端展示区分 User / Bot
3. 挂载幂等（已存在则 no-op）；移除后不可再 @（由 G2 依赖成员关系）

**Non-Goals:**

- @ 解析、Ingress、Runtime、卡片
- 建群时批量挂多个 Bot 的产品化（API 可支持，UI 可后置）
- 外部 installable Bot 市场

## Decisions

### D1 — API 形状

扩展或新增：

| 操作 | 建议 |
|------|------|
| 添加 Bot | `POST /app-api/im/group/bots/add`，body：`conversationId` + `botCode`（或 `botId`） |
| 移除 Bot | `POST /app-api/im/group/bots/remove` |
| 列表 | 扩展现有 `GET …/members`：返回 `subjectType` + Bot 元数据；或并列 `bots` 字段 |

**备选**：复用 `members/add` 传 `botCodes` — 拒绝；人/Bot 校验路径不同，分端点更清晰。

### D2 — 权限

与加人一致：调用方须为群成员；**挂载/移除 Bot** 限 owner（V1 可放宽到任意成员，实现时与 contract 写死一种并测）。推荐 **owner-only**，避免任意成员乱挂。

### D3 — 可达性

系统 Bot（`type=system`）可挂入本租户群，无需 user enablement。非 system Bot：须 tenant 或 user 层已启用（并集，对齐 reach-policy）。

### D4 — 环境文案

挂载成功：`sender_type=system` 文案「{botName} 加入了群聊」；移除可选「…离开了群聊」。**不**走 `ImBotApi`。

### D5 — 前端

`/app/messages` 群详情/成员：展示 Bot 行；提供「添加机器人」选择器（种子 Bot 列表可来自新轻量 API 或写死系统 Bot 列表 + 后端校验）。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 成员 count 含 Bot 导致 UI 困惑 | `memberCount` 可分 `userCount`/`botCount` 或文档标明含 Bot |
| 未做 G2 时挂载无感知价值 | 列表可见 + system 文案；验收不依赖 @ |

## Migration Plan

无 Flyway（列已存在）。回滚：隐藏 API / 前端入口即可。

## Open Questions

| # | 倾向 |
|---|------|
| 挂载权限 owner vs 任意成员 | V1 **owner-only** |
| 是否提供「可挂载 Bot 目录」API | 本切片提供最小 list（系统 Bot）；完整目录后置 |
