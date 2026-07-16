# IM 可交互卡片（实现约定）

> **状态**：设计已拍板（2026-07-16）；**实现**待 OpenSpec change `im-bot-interactive-card`。  
> **背景**：对齐飞书「卡片 = 交互 UI + 回调通道」模型，但 **平台内系统 Bot 走进程内 SPI**，不照搬开放平台「回调 URL / 验签 / 订阅」。  
> **相关**：[`im-messaging-architecture-draft.md`](im-messaging-architecture-draft.md) · [`openspec/lanes/im-bot-dm/contract.md`](../../openspec/lanes/im-bot-dm/contract.md) · 地基 change `im-bot-notify-foundation`

本文描述 **怎么做**。行为规格以 `openspec/specs/im` 及后续 `im-bot-interactive-card` delta 为准。

---

## 1. 一句话定调

> **Card 是 IM 的交互协议与渲染契约；`actionKey` + `payload`（+ 可选 `formValues`）是跨域 opaque 信封；具体语义与副作用由业务模块注册的 `CardActionHandler` 实现。IM 不实现审批/任务/邀请状态机。**

| 角色 | 负责 | 禁止 |
|------|------|------|
| **IM 平台** | 存 card 快照、渲染契约校验、统一 action 入口、鉴权/过期/幂等、路由 SPI、patch 消息、WS 推送 | 写审批/任务等领域逻辑；回潮 `infra_notify` |
| **业务域**（bpm / task / system） | 组装 card、实现 Handler、返回 toast / 新卡快照 | 直写 `im_*`、自推 WS、为每张卡配回调 URL |
| **前端** | 渲染 card；`open_url` 本地跳转；`callback` 调统一 REST | 直调业务域「同意审批」等专用 REST 作为卡片协议 |

---

## 2. 与飞书对照（学什么 / 不学什么）

参考：[配置卡片交互](https://open.feishu.cn/document/feishu-cards/configuring-card-interactions) · [处理卡片回调](https://open.feishu.cn/document/uAjLw4CM/ukzMukzMukzM/feishu-cards/handle-card-callbacks)

| 飞书 | RelayFlow（平台内） |
|------|---------------------|
| `open_url` / `callback` | **采纳**：`open_url` + `callback`（可带 form） |
| `behaviors[].value` opaque | **采纳**：`payload` + `actionKey` |
| 应用级回调 URL + 验签 | **不采纳**（系统 Bot）；进程内 SPI |
| 3s 内响应、toast / 换卡 / 延时更新 | **采纳精神**：统一 action API 同步返回 toast + 可选换卡；延时更新后置 |
| 交互有效期（14–30 天） | **采纳**：`meta.expiresAt`；过期拒绝交互 |
| 外部开发者 webhook | **后置**：`handler_kind=webhook` 时再做 |

---

## 3. 两种投递场景（必须分开）

| | **平台内系统 Bot**（V1 必做） | **外部 / 自定义 Bot**（后置） |
|--|--|--|
| 发送 | `ImBotApi.send`（含 card）进程内 | 外部调 API 发卡 |
| 用户交互 | `POST /app-api/im/card/action` → **SPI Handler** | 同一入口 → HTTP webhook |
| 配置 | 代码注册 `CardActionHandler` bean | URL / 签名 / 超时（开放平台式） |

**铁律**：`org-assistant` / `task-bot` / `approval-bot` 等 `type=system` **禁止**要求配置回调 URL。

---

## 4. 与 Bot Runtime 的关系

| 入站通道 | 触发 | 用途 |
|----------|------|------|
| **Bot Runtime / Ingress** | 用户向 bot_dm 发文本 / 群内 @Bot | 对话能力 |
| **Card Action Ingress** | 用户点击卡片按钮 / 提交表单 | 结构化操作 |

二者都在 `im-biz`，**不要**混成一个 handler。业务域可同时实现「对话」与「卡片动作」，但契约入口分开。

```text
Outbound:  业务域 ──ImBotApi.send(card)──► im-biz ──► im_message + WS

Inbound 对话:  User message / @Bot ──► BotIngress ──► BotRuntime

Inbound 卡片:  User click ──► CardActionIngress ──► CardActionHandler (SPI)
```

---

## 5. 消息模型（Card Document）

### 5.1 存放

- `im_message.type` 可为 `card`（或 `text` 且 blocks 含 `card`；实现切片二选一并写死）
- `content_json.blocks[]` 含 `type=card` 的块
- 地基期已有：`text` + `deeplink` 仍可用；复杂触达再升级为 card

### 5.2 Card 结构（契约草案）

```json
{
  "version": 1,
  "blocks": [
    {
      "type": "card",
      "schema": 1,
      "cardId": "c_xxx",
      "template": "generic.v1",
      "header": {
        "title": "待你审批",
        "subtitle": "采购申请 #1024"
      },
      "fields": [
        { "label": "申请人", "value": "张三" },
        { "label": "金额", "value": "¥12,000" }
      ],
      "actions": [
        {
          "id": "view",
          "label": "查看详情",
          "style": "default",
          "behavior": {
            "type": "open_url",
            "route": "/app/approvals/123"
          }
        },
        {
          "id": "approve",
          "label": "同意",
          "style": "primary",
          "behavior": {
            "type": "callback",
            "actionKey": "bpm.approval.approve",
            "payload": { "instanceId": "123" }
          }
        },
        {
          "id": "reject",
          "label": "拒绝",
          "style": "danger",
          "behavior": {
            "type": "callback",
            "actionKey": "bpm.approval.reject",
            "payload": { "instanceId": "123" },
            "form": [
              {
                "name": "reason",
                "label": "原因",
                "required": true,
                "control": "textarea"
              }
            ]
          }
        }
      ],
      "meta": {
        "expiresAt": "2026-08-15T00:00:00Z",
        "source": {
          "domain": "bpm",
          "entityType": "approval",
          "entityId": "123"
        }
      }
    }
  ]
}
```

| 字段 | 说明 |
|------|------|
| `template` | 前端布局名（如 `generic.v1`）；**不是**业务表结构 |
| `fields` | 展示用 KV；IM 不解析业务含义 |
| `actions[].behavior` | 仅见 §6 |
| `payload` | opaque JSON；发卡时由业务域写入；IM 原样转交 Handler |
| `meta.expiresAt` | 交互截止；超时拒绝 callback |
| `meta.source` | 可选溯源；便于换卡 / 搜索，非鉴权真源 |

### 5.3 模板策略

- V1 推荐默认 **`generic.v1`**（header + fields + actions）覆盖多数场景
- `approval.pending.v1` 等仅为前端皮肤，**不**另开一套交互协议

---

## 6. Behavior 类型（固定集合）

协议层只扩展「注册 handler」，**不**为每个业务加新 REST。

| `behavior.type` | 谁执行 | 请求是否打后端 | 场景 |
|-----------------|--------|----------------|------|
| **`open_url`** | 前端 | 否 | 跳转工作台路由 / 外链（等同现有 deeplink） |
| **`callback`** | 服务端 SPI | 是 | 一键操作；可选带 `form` → 提交时附带 `formValues` |

**表单**：不单独发明 `submit_form` 类型。有输入的操作仍用 `callback`，在 `behavior.form` 声明字段；前端收集后随 action 一并提交。

```text
callback {
  actionKey: "bpm.approval.reject"
  payload:   { instanceId: "123" }      // 发卡时固化
  formValues: { reason: "预算不足" }    // 用户现场填写（可选）
}
```

### 6.1 能力覆盖检查

| 需求 | 如何表达 |
|------|----------|
| 跳转详情页 | `open_url` + `route` |
| 一键同意 / 完成 / 稍后提醒 | `callback` + `actionKey` + `payload` |
| 驳回并填原因 / 延期并选日期 | `callback` + `form` + `formValues` |
| 其它新业务操作 | 新 `actionKey` + 新 Handler；**不改** IM schema |
| 系统外完成后再同步卡片 | 领域事件 → `ImBotApi` 发新卡或 CardUpdate API（非点击回调） |

---

## 7. 平台内调用链（实现目标）

### 7.1 发卡（Outbound）

```text
bpm-biz / task-biz / system-biz
  → ImBotApi.send(botCode, target, text? | card, dedupeKey?, route?…)
  → im-biz：reach 校验 → ensure bot_dm → 落库 im_message
  → RealtimeTransport：domain=im, type=message.new
```

- 跨域仍只依赖 `im-api`，禁止 `*-biz → im-biz`
- `dedupeKey` 规则与现有 Bot 文本触达相同（同窗幂等）

### 7.2 用户交互（Inbound · 系统内 SPI）

```text
前端
  → POST /app-api/im/card/action
  → CardActionIngress（im-biz）
       · JWT 成员校验
       · 消息存在且含目标 action
       · expiresAt / 交互次数（按切片约定）
       · 幂等：clientActionId 或平台 interactionToken
  → CardActionHandlerRegistry.resolve(actionKey)
  → 业务域 Handler（进程内）
  → CardActionResult { toast?, cardPatch? | fullCard? }
  → IM：可选 patch content_json；WS message.updated（或等价）
  → HTTP 响应前端
```

### 7.3 统一 Action API（草案）

```http
POST /app-api/im/card/action
Authorization: Bearer <JWT>
```

```json
{
  "messageId": "…",
  "conversationId": "…",
  "actionId": "reject",
  "actionKey": "bpm.approval.reject",
  "payload": { "instanceId": "123" },
  "formValues": { "reason": "预算不足" },
  "clientActionId": "uuid-for-idempotency"
}
```

成功响应示例：

```json
{
  "code": 0,
  "data": {
    "toast": { "type": "success", "content": "已拒绝" },
    "message": { /* 更新后的 MessageItem；未换卡则可省略 */ }
  }
}
```

`open_url` **不走**本接口。

### 7.4 SPI（`im-api` 契约 · 草案）

```text
CardActionHandler
  actionKey(): String          // 如 "bpm.approval.approve"
  handle(CardActionContext): CardActionResult

CardActionContext
  tenantId, userId, botCode, messageId, conversationId
  actionId, actionKey, payload, formValues, clientActionId

CardActionResult
  toast?: { type, content }
  card?:  完整或 patch 后的 card block（null = 不换卡）
```

- Handler 实现在 **`*-biz`**，接口定义在 **`im-api`**（或 im-api 内 SPI + 业务模块实现并注册）
- 注册方式：Spring bean + `actionKey` 索引（具体实现切片定）
- **禁止**为每个 `actionKey` 再暴露一套 `/app-api/bpm/...` 作为卡片协议入口（业务页自身 CRUD 不受此限）

---

## 8. 响应与更新模式

| 模式 | 说明 | V1 |
|------|------|-----|
| **立即换卡** | Handler 返回新 card → IM patch + WS | ✅ 目标 |
| **仅 toast** | 不改卡，只反馈 | ✅ 目标 |
| **不更新** | 空结果 | ✅ |
| **延时更新** | 先 toast，异步再 patch（飞书 token 模式） | 后置；可用领域事件 + 再调更新 API 代替 |
| **系统外驱动更新** | 审批在页内完成 → 事件 → 更新 bot_dm 卡 | 与点击回调并列支持 |

---

## 9. 安全与约束

| 项 | 约定 |
|----|------|
| 鉴权 | 仅会话 User 成员可对消息发起 action |
| 过期 | `meta.expiresAt` 之后拒绝；前端禁用按钮 |
| 幂等 | `clientActionId`（或 messageId+actionId+token）防重复提交 |
| payload 信任 | Handler **必须**再校验：payload 中的实体是否属于当前用户/租户可操作范围；**不可**仅信卡片里的 id |
| 禁止 | IM 解析 `payload` 业务字段；业务域直写 `im_message` |
| 禁止 | 回潮 `infra_notify` / `domain=notify` 作为业务写真源 |

---

## 10. 前端约定（`/app/messages`）

| 行为 | 实现 |
|------|------|
| 渲染 | 按 `template` / `generic.v1` 画 header、fields、按钮 |
| `open_url` | `router.push(route)` 或打开链接；不请求 card/action |
| `callback` 无 form | 直接 POST card/action |
| `callback` 有 form | 先收集 `formValues`，再 POST |
| 成功 | 展示 toast；若返回 message 则替换本地气泡 |
| 失败 | toast 错误；按钮可恢复可点（按幂等策略） |

轻量触达继续支持只渲染 `text` + `deeplink`，无需强制 card。

---

## 11. 分期落地（建议）

| 阶段 | 范围 | 验证闭环 |
|------|------|----------|
| **地基（已完成）** | 枚举/字段预留 `card`；产方 text + deeplink | — |
| **V1a** | 只读 card + `open_url` 渲染 | 审批/任务卡「查看详情」 |
| **V1b** | `callback` + SPI + toast | **推荐首闭环**：`approval-bot` 同意/拒绝 |
| **V1c** | 立即换卡 + expiresAt + 幂等 | 点后卡片变「已处理」 |
| **V2** | 延时更新、复杂表单控件、webhook Bot | 外部 Bot |

OpenSpec 载体：`im-bot-interactive-card`（母地基 tasks §7.7 开单后实施）。

---

## 12. 模块归属（与架构草案一致）

```text
relayflow-module-im-api
  ImBotApi（发卡）
  CardActionHandler / Context / Result（SPI 契约）

relayflow-module-im-biz
  Card 校验与落库
  CardActionIngress + Registry
  patch 消息 + RealtimeTransport

relayflow-module-bpm-biz / task-biz / system-biz
  组装 card + 实现 CardActionHandler
```

跨域：同步走 `*-api`；异步副作用走领域消息（见 [`cross-domain-messaging.md`](cross-domain-messaging.md)）。

---

## 13. 刻意不做

- 不为平台内 Bot 配置飞书式回调 URL / Verification Token
- 不在 `im-biz` 内置审批/任务状态机
- 不做卡片模板市场 / 可视化搭建器（V1）
- 不为每个业务 action 增加平行「卡片专用」业务 REST 作为协议入口
- 不把 Card Action 与 Bot Runtime 对话入口混成一个接口
- 不回潮 `infra_notify` 双写

---

## 附录：术语

| 术语 | 含义 |
|------|------|
| Card Document | 消息内 `type=card` 的 content 块 |
| Behavior | 按钮交互类型：`open_url` \| `callback` |
| actionKey | 全局路由键，如 `bpm.approval.approve` |
| payload | 发卡时写入的 opaque 业务上下文 |
| formValues | 用户提交时填写的表单值 |
| CardActionHandler | 业务域进程内 SPI 实现 |
| Card Action Ingress | IM 侧统一接收与路由入口 |
