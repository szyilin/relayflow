## Context

- G1 使 Bot 可作为群成员；用户消息需能 @ 触发入站。
- 架构：User → 落库 → Bot Ingress → Bot Runtime；Bot 无人类 WS 接收端。
- Runtime 实装在 `im-bot-runtime-platform`；本切片负责 mention 检测与 Ingress 调用。

## Goals / Non-Goals

**Goals:**

1. 持久化成功后的群消息若 mention 了本群 Bot 成员 → 调用 Ingress
2. Ingress 校验成员关系，组装入站上下文，交给 Runtime dispatcher
3. 不对 Bot subject 发 `message.new` 客户端推送

**Non-Goals:**

- Runtime handler 业务逻辑（归 G3）
- 卡片 action（归 interactive-card）
- 全站富文本编辑器大重构（最小 mention 即可）

## Decisions

### D1 — Mention 表示

优先结构化：`content_json.blocks` 含 `type=mention`，`subjectType=bot`，`subjectId`/`botCode`。  
纯文本 `@BotName` 可作为兼容解析，但 **真源以结构化块为准**（contract 写死）。

### D2 — 触发时机

**先持久化再 Ingress**（对齐「先持久化再确认」）：用户消息对真人成员已可见；Runtime 异步/同步回复为后续消息。V1 可用同步调用 Runtime（失败只打日志，不回滚用户消息）。

### D3 — 多 Bot

一条消息可 @ 多个 Bot：对每个命中成员各调一次 Ingress。

### D4 — 与 Runtime 耦合

```text
Ingress.onBotMentioned(ctx) → BotRuntime.dispatch(ctx)
```

若 Runtime 尚未部署：dispatcher 默认 noop，保证编译与冒烟。合入 G3 后替换实现。

### D5 — 前端

发送消息时插入 mention 块；气泡渲染 @ 高亮。可选：从群成员 Bot 列表选人。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 同步 Runtime 拖慢发送 | 超时短 + catch；后续改异步领域消息 |
| 误 @ 未挂载 Bot | Ingress 校验成员，忽略或记日志 |

## Migration Plan

无 schema 硬切。回滚：去掉发送路径钩子。

## Open Questions

（无；依赖 G1/G3 顺序在 tasks 注明）
