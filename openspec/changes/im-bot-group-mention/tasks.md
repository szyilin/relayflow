# Tasks：im-bot-group-mention（G2）

> 验证：`openspec validate im-bot-group-mention --strict`；compile + 相关单测；含 web 时 `pnpm build`  
> 依赖：`im-bot-group-member`（Bot 已是群成员）；Runtime 建议 `im-bot-runtime-platform` 同窗口或先合入

## 1. Mention 与 Ingress

- [x] 1.1 约定 `mention` content block schema（写入 lane contract）
- [x] 1.2 群消息发送成功后：解析 mention → 过滤本群 Bot 成员 → 调 Bot Ingress
- [x] 1.3 Ingress：组装上下文；调用 Runtime.dispatch；失败 catch 不回滚用户消息
- [x] 1.4 确认 realtime fanout **不对** Bot subject 推客户端信封 — 沿用 `listMemberUserIds` / `listOtherMemberUserIds`
- [x] 1.5 单测：命中 / 未挂载忽略 / Runtime 抛错不丢消息 — `GroupBotMentionDispatcherTest`

## 2. 前端（最小）

- [x] 2.1 发送群消息可插入 Bot mention（从群 Bot 成员选）
- [x] 2.2 气泡渲染 @Bot；`pnpm build` + `pnpm typecheck`；冒烟：群侧栏挂 Bot → 输入栏点 @ → 发送 → 侧栏/历史见高亮

## 3. 收口

- [x] 3.1 `openspec validate im-bot-group-mention --strict`
- [x] 3.2 更新对接看板；foundation §7.5 已在 archive 勾选
- [x] 3.3 `./mvnw -pl relayflow-server -am compile` 通过
