## Context

- 架构草案 §9：Bot Runtime 按 `handler_kind` 分发；与 Outbound `ImBotApi`、Card Action 三分。
- `im_bot.handler_kind` 已有列；种子多为系统助手，适合 `noop` inbound。
- Ingress（G2）需要可调用的 dispatcher。

## Goals / Non-Goals

**Goals:**

1. SPI：`BotHandler` / `BotRuntime.dispatch(InboundContext)`
2. `noop`、`platform` 可运行；`webhook` 占位（调用时明确未实现）
3. 回复写入同一 `conversationId`，`sender_type=bot`，WS 仅 User
4. bot_dm 文本入站也可走同一 Runtime（可选挂钩，验收以 SPI + noop/platform 单测为主）

**Non-Goals:**

- 外部 HTTP webhook 实装、签名、重试队列
- 业务域实现「收 @」（task/bpm 禁止）
- 可交互卡片 SPI（独立）

## Decisions

### D1 — 接口归属

契约在 `im-api`（或 im 内部 api 包，若不想污染跨域：可放 `im-biz` 内部 SPI + 测试可见）。  
跨域业务 **不**依赖 Runtime 接口；仅 im 内 Ingress 使用。倾向：**im-biz 内 SPI + Spring 注入**，避免无意义的跨模块 API 面。若需业务注册 platform handler，再把接口提到 `im-api`。

V1 系统 Bot 以 `noop` 为主 → **SPI 先放 im-biz**；`platform` 用 im 内 demo handler 验证通路。文档注明升级路径。

### D2 — handler_kind

| kind | 行为 |
|------|------|
| `noop` | 返回空，不发回复 |
| `platform` | 查注册表；无 handler 则等同 noop |
| `webhook` | 抛「未实现」或记日志返回；**不**发 HTTP |

### D3 — 回复路径

Runtime 回复调用内部 `MessageService`（同会话），**不**强制经 `ImBotApi.send`（`ImBotApi` 面向跨域 bot_dm 触达）。群回复直接落群消息。

### D4 — 与 Card 分离

CardActionHandler **禁止**实现为 BotHandler；文档与包名分开。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| platform 范围膨胀 | V1 仅 demo/echo；业务听事件 + ImBotApi |
| webhook 被误用 | 明确 stub + 测试断言不发起 HTTP |

## Migration Plan

无 Flyway。回滚：Ingress 改回空实现。

## Open Questions

| # | 倾向 |
|---|------|
| platform demo 是否合入主路径 | 测试用 bean；生产种子保持 noop |
