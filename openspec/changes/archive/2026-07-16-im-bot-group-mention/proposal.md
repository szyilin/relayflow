## Why

群挂载 Bot（G1）之后，用户需要能在群内 @ 机器人触发入站处理。地基已规划「@ → Ingress → Runtime」，本切片落地 mention 检测与 Ingress 调用，打通群内对话入站路径。

## What Changes

- 群消息落库后：若 content 含对已挂载 Bot 的 mention（`mention` 块或约定 @ 语法），调用 Bot Ingress
- Ingress 解析目标 `botCode` / `botId`，校验 Bot 仍为群成员，再分发给 Bot Runtime
- **禁止**向 Bot 成员推送人类客户端 WS 信封（Bot 无登录端）
- 依赖 Runtime SPI：若 `im-bot-runtime-platform` 尚未合入，Ingress 可调用占位 dispatcher（noop）；合入后切换真实 SPI
- **不**实现卡片 callback、外部 webhook 实装、群外 bot_dm 文本入站的产品化（bot_dm 入站可同走 Ingress，但本切片验收以群 @ 为主）

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `im`：群内 @Bot 触发 Ingress；明确与 Runtime 的边界及「不对 Bot 推客户端」

## Impact

- `im-biz`：消息发送路径挂钩 Ingress；mention 解析
- `im-api`：可暴露 Ingress 内部契约（非跨域业务 API）
- 依赖：`im-bot-group-member`（Bot 须先为群成员）；建议与 `im-bot-runtime-platform` 联调或先后合入
- 前端：消息输入支持 @Bot 选择（或最小可用的 mention 块）；展示 @ 气泡
