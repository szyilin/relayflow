## Why

地基已预留 `card` 内容形态，产方目前只能发 text + deep link。要对齐飞书式可交互卡片（只读卡、一键操作、换卡），需落地卡片协议、统一 action API 与进程内 `CardActionHandler` SPI。实现约定已拍板于 `docs/dev/im-bot-interactive-card.md`。

## What Changes

- `ImBotApi.send` 支持发送 `card` 块（`generic.v1` 模板）；落库 `im_message` + WS
- 前端 `/app/messages`：渲染 card；`open_url` 本地跳转；`callback` 调统一 REST
- `POST /app-api/im/card/action` → CardActionIngress → `CardActionHandler`（`im-api` SPI）
- 鉴权 / `expiresAt` / `clientActionId` 幂等；成功可 toast + 立即换卡
- 首闭环建议：`approval-bot` 或演示 Handler（若 bpm 未就绪可用 im 内 demo handler）
- **禁止**回潮 `infra_notify`；**禁止**系统 Bot 要求回调 URL
- **不**做延时更新 token、外部 webhook Bot、复杂表单控件全集（后置）

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `im`：可交互卡片发送、渲染契约、统一 action 入口与 SPI；将地基「Card content placeholder」升级为可交互行为（前端 `/app/messages` 渲染与交互写入本 capability + lane contract）

## Impact

- `im-api`：`CardActionHandler` / Context / Result；`ImBotApi` card 载荷
- `im-biz`：校验、Ingress、Registry、patch + WS
- `web/`：card 气泡组件 + action 客户端
- 业务域（后续）：bpm/task/system 实现 Handler；本切片至少一条可测通路
- 真源文档：`docs/dev/im-bot-interactive-card.md`
