# Tasks：im-bot-runtime-platform（G3）

> 验证：`openspec validate im-bot-runtime-platform --strict`；`./mvnw -pl relayflow-module-im/relayflow-module-im-biz -am test`（相关用例）；server compile  
> 建议先于或并行于 `im-bot-group-mention` 合入，避免 Ingress 悬空

## 1. SPI 与内置 kind

- [x] 1.1 定义 InboundContext + BotRuntime / Handler 接口（im-biz 内 SPI；文档写明升级 im-api 条件）
- [x] 1.2 实现 `noop` dispatcher 分支
- [x] 1.3 实现 `platform` 注册表 + 缺失 handler 等同 noop；可选 demo/echo handler（测试用）
- [x] 1.4 `webhook` stub：不发起 HTTP；单测断言
- [x] 1.5 回复落同一会话 `sender_type=bot`；WS 仅 User 成员

## 2. 接线与测试

- [x] 2.1 与 Bot Ingress 接线（若 G2 已存在）或提供可测入口 — `BotIngress.onInbound` → Runtime
- [x] 2.2 单测：noop / platform / webhook stub / 回复 fanout — `BotRuntimeImplTest`
- [x] 2.3 `./mvnw -pl relayflow-server -am compile` 通过

## 3. 收口

- [x] 3.1 `openspec validate im-bot-runtime-platform --strict`
- [x] 3.2 更新 `docs/dev/im-messaging-architecture-draft.md` 若实现细节与草案冲突
- [x] 3.3 更新对接看板；foundation §7.6 已在 archive 勾选
