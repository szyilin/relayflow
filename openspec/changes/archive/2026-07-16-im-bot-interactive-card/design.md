## Context

- 约定真源：[`docs/dev/im-bot-interactive-card.md`](../../../docs/dev/im-bot-interactive-card.md)
- 地基已预留 `card`；产方 text+deeplink 已跑通
- 与 Bot Runtime **分离**：对话入站 ≠ 卡片动作

## Goals / Non-Goals

**Goals:**

1. 发卡：`ImBotApi.send` 支持 card document（`generic.v1`）
2. 前端渲染 + `open_url` / `callback`
3. `POST /app-api/im/card/action` + CardActionIngress + SPI
4. expiresAt、成员鉴权、clientActionId 幂等；toast + 立即换卡
5. 至少一条端到端通路（demo Handler 或 approval-bot）

**Non-Goals:**

- 延时更新 token、webhook Bot、开放平台回调 URL
- 用卡片协议替代业务页自身 CRUD REST
- 回潮 notify

## Decisions

### D1 — 协议

完全按 `im-bot-interactive-card.md` §5–§7：behavior 仅 `open_url` | `callback`；form 挂在 callback 上。

### D2 — 存放

`im_message.type=card`（或 text+card block，实现时二选一并写死 contract）。推荐 **type=card** + `content_json.blocks` 含 card。

### D3 — SPI 位置

`CardActionHandler` 在 **`im-api`**，业务 `*-biz` 实现并注册为 Spring bean。IM 不解析 payload 业务字段。

### D4 — 分期验收（本 change 内）

| 阶段 | 本 change |
|------|-----------|
| V1a 只读 + open_url | ✅ |
| V1b callback + toast | ✅ |
| V1c 换卡 + expires + 幂等 | ✅ 目标 |
| V2 延时/webhook | ❌ |

### D5 — 首闭环 Handler

优先：若 `bpm-v1` 进度允许 → `bpm.approval.approve/reject`。  
否则：im-biz 内 `demo.card.ack` Handler，仅用于联调，文档标明非产品能力。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| payload 伪造 | Handler 必须重鉴权实体 |
| 前端与协议漂移 | lane contract + 示例 JSON 冻结 |

## Migration Plan

无删表。旧 text+deeplink 消息继续有效。回滚：隐藏 card 渲染与 action API。

## Open Questions

| # | 倾向 |
|---|------|
| 首闭环用 bpm 还是 demo | 实现时看 bpm 进度；tasks 允许 demo 兜底 |
