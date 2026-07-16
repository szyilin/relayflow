# Tasks：im-bot-interactive-card

> 验证：`openspec validate im-bot-interactive-card --strict`；server compile；`cd web && pnpm build && pnpm typecheck`  
> 真源：`docs/dev/im-bot-interactive-card.md`；与 Bot Runtime **分开**实现

## 1. 契约与发卡

- [x] 1.1 冻结 lane contract（card document + `/app-api/im/card/action` 请求/响应）
- [x] 1.2 `ImBotApi.send` 支持 card 载荷；校验 `generic.v1` 必填字段
- [x] 1.3 落库 + `domain=im, type=message.new`；单测发卡

## 2. Action SPI 与 Ingress

- [x] 2.1 `im-api`：`CardActionHandler` / Context / Result
- [x] 2.2 `CardActionIngress`：成员鉴权、expiresAt、clientActionId 幂等、Registry
- [x] 2.3 `POST /app-api/im/card/action`；toast + 可选 patch 卡 + WS `message.updated`（或等价）
- [x] 2.4 Demo 或 bpm Handler 至少一条可测 callback；**禁止**回调 URL

## 3. 前端

- [x] 3.1 `/app/messages` 渲染 `generic.v1`（header/fields/actions）
- [x] 3.2 `open_url` 本地跳转；`callback`（含可选 form）调统一 API
- [x] 3.3 成功 toast / 换卡；过期禁用；`pnpm build` + `pnpm typecheck` + 浏览器路径

## 4. 收口

- [x] 4.1 文档确认与 `im-bot-interactive-card.md` 一致；禁止回潮 notify
- [x] 4.2 `openspec validate im-bot-interactive-card --strict`；看板更新；勾选 foundation §7.7
- [x] 4.3 server compile + web build 通过
