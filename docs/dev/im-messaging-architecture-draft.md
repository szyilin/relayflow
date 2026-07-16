# IM 即时通讯架构草案（已拍板）

> **状态**：**已拍板 · 地基已 archive**。可执行真源 → OpenSpec archive [`im-bot-notify-foundation`](../../openspec/changes/archive/2026-07-16-im-bot-notify-foundation/proposal.md)；主规格见 `openspec/specs/im`。后续实现：`im-bot-group-member` / `im-bot-group-mention` / `im-bot-runtime-platform` / `im-bot-interactive-card`。  
> **用途**：背景与概念长文；实现以 OpenSpec tasks 为准。  
> **整理日期**：2026-07-15 · **拍板**：2026-07-15/16  
> **相关**：`openspec/specs/im|infra|system|task|web-auth` delta 见上述 change；`notify-inbox-v2` 写真源 **SUPERSEDED**；空壳 `workspace-notify-system-thread` 已删除  
> **历史设计**：`openspec/changes/archive/2026-07-12-im-platform-foundation/design.md`（双通道真源，已被本方向修订）

---

## 目录

1. [问题动机](#1-问题动机)
2. [现状架构（as-is）](#2-现状架构as-is)
3. [飞书对照与概念澄清](#3-飞书对照与概念澄清)
4. [目标原则](#4-目标原则)
5. [三层主体：身份 / 成员 / IM 上下文](#5-三层主体身份--成员--im-上下文)
6. [参与者与会话类型](#6-参与者与会话类型)
7. [机器人模型](#7-机器人模型)
8. [消息与实时](#8-消息与实时)
9. [Bot Runtime（入站处理）](#9-bot-runtime入站处理)
10. [跨域调用组织](#10-跨域调用组织)
11. [完整分层图](#11-完整分层图)
12. [与现状资产的映射（删 / 留 / 迁）](#12-与现状资产的映射删--留--迁)
13. [平台 Bot 起步清单](#13-平台-bot-起步清单)
14. [端到端示例](#14-端到端示例)
15. [对讨论建议的取舍](#15-对讨论建议的取舍)
16. [刻意不做](#16-刻意不做)
17. [已拍板事项](#17-已拍板事项)
18. [后续工作](#18-后续工作)
19. [可交互卡片](#19-可交互卡片)

---

## 1. 问题动机

> **以下「双通道」描述为 2026-07-16 地基交付前的历史 as-is**；当前写真源已统一为 Bot / `bot_dm`（见 §17 拍板结论与 OpenSpec `im-bot-notify-foundation`）。

当前「业务触达」与「IM 会话」曾被拆成两条通道：

- 人聊 / 群聊 / 群内环境文案 → `im_*`
- 邀请 / 任务到期 /（规划中）审批待办 → `infra_notify` + Rail 铃铛

这与飞书常见模型不一致：飞书侧大量「系统通知 / 业务提醒」表现为**应用机器人向用户发的会话消息**，而不是独立于 IM 的第二套收件箱写模型。

本草案目标：

1. 把**业务触达统一收进 IM 的 Bot 能力**；
2. 澄清多企业下「账号级 vs 企业级」投递；
3. 给其它模块一个唯一、合理的跨域 API；
4. 标明应删除 / 废弃的双通道资产；
5. 为将来自定义机器人预留 Runtime 接口（先占位）。

---

## 2. 现状架构（as-is · 历史，已废弃）

> **本节仅作迁移前快照**；`infra_notify`、Rail 铃铛、`NotifyInboxApi` 已在 `im-bot-notify-foundation` 硬切拆除。现行行为以 `openspec/specs/im|infra|system|task|web-auth` 为准。

### 2.1 双通道总览

```text
┌─────────────── 业务模块 ───────────────┐
│  system（邀请）  task（到期）  bpm（规划） │
└────────────┬──────────────┬────────────┘
             │              │
             ▼              ▼（几乎没有）
    NotifyInboxApi      ImMessageApi（规格有，代码基本无）
             │
             ▼
      infra_notify 表
      WS domain=notify
      Rail 铃铛

人 ↔ 人 / 群聊 ──────────────► im_message
群「XX 加入」 ────────────────► im_message (sender_type=system)
Bot / channel ────────────────► 仅 schema 预留，无产品
```

| 通道 | 存储 | 用户可见 | 谁在写 |
|------|------|----------|--------|
| **IM** | `im_*` | `/app/messages` | 用户 + 群内 system 文案 |
| **Notify Inbox** | `infra_notify` | Rail 铃铛 | `system` / `task` 经 `NotifyInboxApi` |

设计来源：`im-platform-foundation` 事件分类表将「审批 / 任务 / @」归入 notify，并写明**通知不得写入 `im_message`**。因此问题不是实现跑偏，而是**产品抽象把业务触达与会话拆开了**。

### 2.2 IM 模块现状要点

| 项 | 现状 |
|----|------|
| 表 | `im_conversation` / `im_conversation_member` / `im_message` / `im_group` / `im_channel`（channel 仅 schema） |
| 会话 type | DB：`direct \| group \| channel`；Java 枚举目前主要用 DIRECT、GROUP |
| `sender_type` | 预留 `user \| system \| bot \| app`；实现主要是 **user + system** |
| 系统消息 | `ImMessageService.sendSystemMessage`（biz 内部）；调用方主要为群加入 |
| Bot | **无产品**：无 bot 表、无服务、无管理 |
| 跨域 API | `ImConversationApi` 仅有搜索等；`getOrCreateDirect` / `ImMessageApi` 规格有、实现缺口大 |
| 实时 | WS `domain=im`（`message.new`、`read.updated` 等） |

### 2.3 Notify Inbox 现状要点

| 项 | 现状 |
|----|------|
| 表 | `infra_notify`（含 `dedupe_key`） |
| 类型目录 | `MEMBER_INVITE`、`TASK_DUE` 已用；`TASK_ASSIGNED` / `IM_MENTION` / `APPROVAL_PENDING` 预留 |
| API | `NotifyInboxApi.push` / `backfillUserIdByMobile` / `hasUnreadDedupe` |
| REST | `/app-api/infra/notify/*` |
| 实时 | WS `domain=notify`，`notify.new` |
| 生产方 | `system-biz`（邀请）、`task-biz`（到期）；bpm 规划消费 notify |

### 2.4 跨域触发方式

- 当前以 **同步 `*-api`** 为主，**不是**领域事件总线。
- `docs/dev/cross-domain-messaging.md` 描述了异步领域消息，但业务 notify/IM 触达尚未系统用起来。
- IM **没有**对外业务写入口；群 system 文案只在 im-biz 内部。

### 2.5 已知缺口与重叠

| 缺口 | 说明 |
|------|------|
| 双通道 | 聊天 ≠ 产品收件箱，用户心智与飞书不一致 |
| 「system」一词过载 | IM 群内文案 vs Notify 业务提醒，同名不同仓 |
| Bot 未落地 | schema/spec 预留而已 |
| `ImMessageApi` 缺失 | 跨模块卡片/系统投递进会话受阻 |
| Spec 漂移 | 已同步：`im/spec.md` 等主规格已移除 NotifyInboxApi 占位、业务触达改 `ImBotApi` |
| 空壳 change | `workspace-notify-system-thread` 无 proposal/tasks，方向未建模 |

---

## 3. 飞书对照与概念澄清

### 3.1 飞书常见做法（对齐目标）

1. **机器人是会话参与者**（可私信、可进群）。
2. **业务通知 ≈ 某机器人发给你的一条（或卡片）消息**。
3. 顶部铃铛多半是**未读聚合 / 快捷入口**，真源仍是机器人会话中的消息。
4. 群里「张三加入了群」仍是**会话内环境文案**，一般不当作独立「通知产品」。

### 3.2 必须拆开的两个概念

| 概念 | 飞书直觉 | 本草案归属 |
|------|----------|------------|
| **会话环境消息** | 「XX 加入群」 | `sender_type=system`，只活在该群时间线 |
| **业务触达** | 任务 / 审批 / 邀请机器人私信 | `sender_type=bot`，写入 User↔Bot 会话 |

「没有系统通知」更准确的说法是：

> **没有第二条业务写路径**；触达统一经 IM 的 Bot 能力。

### 3.3 Bot 归属：平台主体 vs 启用关系

**Bot 本身是平台级主体**，由 `im_bot` 等表维护（Flyway 种子 + 后续管理）；定义挂在平台目录，**不归属某个 tenant**。成员能否看见、能否被推送，取决于在该企业下的 **enablement** 关系。

**V1**：仅平台内置系统 Bot（见 §13），`mandatory` / `default_on` 策略下成员入企自动具备 user enable，全员可触达。  
**后期**（本阶段不实现、本文不展开）：成员按需启用外部 Bot；schema 可预留 `opt_in` / `installable`，产品另立项。

---

## 4. 目标原则

| # | 原则 | 含义 |
|---|------|------|
| P1 | **参与者两类** | `User`（真人）与 `Bot`（特殊主体）；都可作为 `sender` |
| P2 | **会话是唯一聊天真源** | 单聊 / 群 / Bot 私信 / 频道，都落在 `im_conversation` + `im_message` |
| P3 | **无第二条业务通知总线** | 废弃以 `infra_notify` 为真源的写路径；业务触达 = Bot 发消息 |
| P4 | **IM 永远在企业上下文中** | 与产品模型一致：JWT 必有 `tenant_id`；`im_*` 必有 `tenant_id`；不存在「无主账号 inbox」 |
| P5 | **Bot 没有人类接收端** | 入站消息交给 **Bot Runtime**，不是推给真人客户端 |
| P6 | **跨域只调 `im-api`** | `system` / `task` / `bpm` 不直写 `im_*`，不依赖 `im-biz` |

「订阅」易与推送/邮件混淆。本草案优先使用行业更常见的说法：

- **安装 / 启用（Enablement / Install）**
- **可见性（Visibility）**

少用「订阅」作为主术语。启用关系分 **tenant 层**（企业是否允许）与 **user 层**（成员是否已具备资格；V1 由入企自动写入），见 §7。

---

## 5. 三层主体：身份 / 成员 / IM 上下文

这是解开「账号级机器人 vs 企业级机器人」混乱的关键。

```text
┌─────────────────────────────────────────────────────────┐
│ Identity（身份）  sys_user                                │
│   登录凭据、手机号；跨企业同一人                             │
│   ❌ 不挂 im_conversation / im_message                     │
└────────────────────────┬────────────────────────────────┘
                         │ 1 : N
┌────────────────────────▼────────────────────────────────┐
│ Membership（企业成员） sys_tenant_user                     │
│   (tenant_id, user_id, status)                          │
│   工作台永远处在「当前选中的企业」                         │
└────────────────────────┬────────────────────────────────┘
                         │ 会话归属
┌────────────────────────▼────────────────────────────────┐
│ IM Context（本企业内的聊天世界）                           │
│   conversation / message / bot tenant·user enablement / unread      │
│   永远带着 tenant_id                                      │
└─────────────────────────────────────────────────────────┘
```

与飞书及现有 `product-permission-model` 对齐：

- 一账号可多企业；
- 任一时刻必在某一企业（JWT `tenant_id`）；
- 聊天列表、未读、Bot 会话都是**当前企业视角**。

### 5.1 对「账号级通知」的改写

| 直觉说法 | 更合理的系统表述 |
|----------|------------------|
| 「账号安全中心」属于账号 | **Bot 目录可平台级**；**消息仍写入各企业下的 Bot 会话**（按策略扇出） |
| 「机器人给所有企业都发了」 | **一次 Identity 事件 → 对每个 ACTIVE membership 各投递一次** |
| 「像发给两个不同账号」 | **像发给两个收件上下文**：`(T_A, U)` 与 `(T_B, U)`，同 `user_id`、不同 `tenant_id` |
| 「角色变更只该出现在 A」 | **Tenant 作用域事件**：只投递 `(T_A, U)` |

**结论**：没有「无 tenant 的 IM 表」。若将来要「身份全局聚合页」，也是**读模型**聚合各企业未读，而不是第三张通知写表。

从 Bot API 视角：**始终面向 `(tenantId, userId)`**；全企业扇出只是调用方或框架循环，不是「一个无租户超级会话」。

---

## 6. 参与者与会话类型

### 6.1 Participant

```text
Participant
  ├── User   subject_type=user, subject_id=userId
  └── Bot    subject_type=bot,  subject_id=botId
```

- 消息：`sender_type` + `sender_id` 表示谁发出。
- 单聊「接收方」由**会话成员**决定谁能看见，不必在每条消息上再造接收人字段（direct 场景语义仍清晰：两人互为 peer）。

### 6.2 Conversation types

| type | 成员 | 谁能发 | 说明 |
|------|------|--------|------|
| `direct` | 恰好 2 个 User | 两名 User | 普通用户私信，发送人/接收人清晰 |
| `bot_dm` | 1 User + 1 Bot | User 可问；Bot 可答/主动推 | **业务触达主通道** |
| `group` | N User + 可选若干 Bot | User；Bot 需被 @ 或规则触发才回 | 聚合聊天空间 |
| `channel` | 后置 | 后置 | 与通知重构无关，保持预留 |

群内「张三加入」继续用 **`sender_type=system`**（环境文案），不是 Bot，也不走业务触达总线。

```text
direct:     User A ←→ User B
bot_dm:     User U ←→ Bot X          （X 无真人在线端）
group:      Users… + optional Bots   （@Bot → Runtime）
```

### 6.3 Bot 为何「像用户」却不是 `sys_user`

| | User | Bot |
|--|------|-----|
| 出现在成员列表 | ✅ | ✅ |
| 可作 sender | ✅ | ✅ |
| 有 JWT / 可登录客户端 | ✅ | ❌ |
| 收消息去哪 | 用户客户端 + WS | **Bot Runtime** |
| 是否占 `sys_user` | ✅ | ❌（独立 `im_bot`，避免污染账号体系） |

**交互语义上是参与者，身份体系上不是真人账号。**

---

## 7. 机器人模型

> **真源补充**：触达判定见 OpenSpec [`im-bot-reach-policy-v1`](../../openspec/changes/im-bot-reach-policy-v1/design.md)。

### 7.1 目录、启用、会话

```text
┌─────────────────────────────────────┐
│ Bot Definition（平台目录 · im_bot）   │
│  code, name, type(system|tenant),    │
│  scope, enable_policy, handler_kind  │
│  V1：种子维护系统默认 Bot（type=system）│
└─────────────────┬───────────────────┘
                  │
     type=system  │  type=tenant（非 system）
        │         │
        │         ├─► Tenant Enablement (tenant, bot)  ─┐
        │         │                                      │  并集任一即可投递
        │         └─► User Enablement (tenant,user,bot) ─┘
        │
        ▼ 免查订阅表
┌─────────────────────────────────────────────────────┐
│ bot_dm Conversation（懒创建 ensure）                    │
│  unique (tenant_id, bot_id, user_id)                 │
└─────────────────────────────────────────────────────┘
```

| Bot type | 订阅表 | 谁可被推送 |
|----------|--------|------------|
| **`system`** | **不查** tenant / user 订阅 | 任意企业上下文下的合法投递目标（扇出仍受 ACTIVE membership 约束） |
| **`tenant`**（非 system） | **并集**：企业订阅 **或** 用户订阅 | 有任一订阅即可；**不**再入企把企业订阅拷贝到用户表 |

私信会话一律 **懒创建（ensure）**。系统 Bot（组织助手等）**不要求** 每企业种子 `im_bot_tenant_enablement`，也不要求入企写 `im_bot_user_enablement`。

### 7.2 scope（作用域）

| scope | 含义 | V1 |
|-------|------|-----|
| `tenant` | 任务 / 审批 / 组织助手等 | ✅ 多为 `type=system` |
| `identity_fanout` | 账号安全、跨企业邀请等 | ✅ 扇出到各 ACTIVE membership |
| `installable` | 外部可安装 Bot | ❌ 仅占位 |

投递默认：`tenant` 只写**事件所在 tenant**；`identity_fanout` / 调用方选 `ALL_ACTIVE_MEMBERSHIPS` → **每个 ACTIVE membership 各投一次**。

### 7.3 enable_policy（启用策略）

`enable_policy` 仍用于描述「如何开通」的扩展语义；**发送门禁以 `type` 为准**。

| 策略 | 说明 | V1 |
|------|------|-----|
| `mandatory` / `default_on` | 历史字段；system Bot 可不依赖 | 种子可保留 |
| `opt_in` | 用户主动订阅（写 user 表） | ❌ 仅占位 |

**非 system Bot**：企业订阅与用户订阅为 **并集**，不必双层都写。

### 7.4 何时发生 Enablement 与会话 ensure（V1）

```text
1. 用户加入企业（membership → ACTIVE）
   → type=system：不写 user enablement
   → type=tenant 且企业已订阅 + policy 要求时：可写 user enable（后置；当前非必须）

2. 首次 ImBotApi.send / 用户打开 Bot
   → type=system：直接 ensure bot_dm + 落库
   → type=tenant：校验 tenant ∪ user 订阅后 ensure bot_dm

3. Identity 扇出（invite 等）
   → 枚举 ACTIVE tenants → 各写一条（system 免订阅）
   → 产方应对 send 失败 best-effort（catch，不挡主业务）
```

### 7.5 同一 API，不同 Fanout

```text
ImBotApi.send(SendCommand)
  botCode, content, dedupeKey?,
  target: { tenantId, userId }                         // 最常用：本企业
  // 或
  target: { userId, fanout: ALL_ACTIVE_MEMBERSHIPS }   // Identity 事件
```

| 场景 | 调用方 | target |
|------|--------|--------|
| A 企业管理员改了角色 | `system` @ tenant A | `{ tenantId:A, userId }` |
| A 企业任务到期 | `task` | `{ tenantId:A, userId }` |
| B 企业邀请你（人正在 A） | `system` | `{ userId, fanout:ALL_ACTIVE }` → A、C… 各一条 bot_dm |
| 密码异常等 | security | 同上 fanout（或仅当前 JWT tenant，产品可选，见 §17） |

### 7.6 后期扩展（V1 不实现）

schema / 枚举可预留 `scope=installable`、`enable_policy=opt_in`、`handler_kind=webhook`，供将来外部 Bot 与 Runtime 扩展；**本阶段无对应产品入口与 API**，细节另立项，本文不展开。

---

## 8. 消息与实时

### 8.1 Message 模型（目标）

```text
im_message
  conversation_id, seq, client_msg_id
  sender_type: user | bot | system
  sender_id:   userId | botId | 0
  type: text | image | file | card | system
  content_json (Content Blocks)
  tenant_id
```

| sender_type | 用途 |
|-------------|------|
| `user` | 真人发送 |
| `bot` | 业务触达与 Bot 回复 |
| `system` | **仅**会话环境文案（加群等）；**禁止**业务模块当通知总线 |

幂等（承接现 `dedupe_key` 思路）：  
`(tenant_id, bot_id, user_id, dedupe_key)` 在未读/约定窗口内唯一。

### 8.2 实时

```text
落库成功 → RealtimeTransport
  domain=im
  type=message.new | read.updated | …
  目标：会话内 User 成员（Bot 成员不推客户端）
```

`domain=notify`：**目标态不再作为业务真源**。若保留 Rail，只作为 bot_dm 未读的**读聚合**（可选）。

---

## 9. Bot Runtime（入站处理）

Bot 收消息不是人收，而是入站管道。

```text
                    ┌──────────────────────┐
  User → bot_dm 或   │   Bot Ingress         │
  group @Bot         │  (im-biz)             │
                    └──────────┬───────────┘
                               ▼
                    ┌──────────────────────┐
                    │ Bot Runtime          │
                    │  resolve bot + cfg   │
                    │  dispatch by kind    │
                    └──────────┬───────────┘
           ┌───────────────────┼───────────────────┐
           ▼                   ▼                   ▼
    platform_handler    outbound_webhook     (future) …
    （内置能力）          （自定义 Bot 占位）
           │                   │
           └─────────┬─────────┘
                     ▼
              回复到同一 conversation
              （经 MessageService / ImBotApi）
```

### 9.1 handler_kind

| kind | V1 | 说明 |
|------|----|------|
| `platform` | ✅ | 内置 Handler；可为 noop inbound，只消费主动推送 |
| `webhook` | 占位 | 自定义 Bot：HTTP 回调外部；签名、重试、超时写入契约，实现后置 |
| `noop` | ✅ | 仅主动推送、不处理入站（大量系统助手如此） |

### 9.2 两套入口必须分开

| 入口 | 谁发起 | 典型 |
|------|--------|------|
| **Outbound（主动触达）** | 业务模块 → `ImBotApi.send` | 任务到期、角色变更、邀请 |
| **Inbound（对话能力）** | 用户发消息 / @Bot → Runtime | 「查待办」、自定义 webhook |

前期系统机器人可以 **几乎只有 Outbound + noop inbound**；Ingress / Runtime SPI 仍要先定，方便以后自定义 Bot。

**实现状态（2026-07-16）**：`im-bot-runtime-platform` 已落地 im-biz 内 SPI：

- `BotIngress` → `BotRuntime.dispatch`（按 `handler_kind`）
- `noop` / `platform`（`BotPlatformHandler` 注册表；缺 handler = noop）/ `webhook`（仅日志 stub，**不**发 HTTP）
- 回复：`ImMessageService.sendBotReply`（同会话 `sender_type=bot`，WS 仅 User 成员）
- 跨域业务仍只走 `ImBotApi.send`；群 @ 接线见 `im-bot-group-mention`
- Card Action **独立** Ingress（`im-bot-interactive-card`），禁止与 BotHandler 混用
- Platform handler 若需业务模块注册，再将 SPI 升到 `im-api`（V1 默认系统 Bot 为 noop）

### 9.3 模块归属（建议）

```text
relayflow-module-im
  ├── Bot Definition / Enablement / Conversation ensure
  ├── ImBotApi（跨域唯一业务触达入口）
  ├── Bot Ingress
  └── Bot Runtime SPI（platform handlers；厚业务也可听领域事件再调 ImBotApi）

自定义 / 外部能力：im-api 上 Webhook 契约占位
```

业务域（task/bpm）**不要**实现「收 @」；它们产出事实 → 最终 **`ImBotApi.send`**。

---

## 10. 跨域调用组织

| 模式 | 何时 | 例子 |
|------|------|------|
| **同步 `ImBotApi.send`** | 与当前请求强相关、要立刻可见 | 邀请创建后推一条 |
| **本域事件 → im 消费 → send** | 一对多副作用、不阻塞主路径 | `task.due` / `bpm.todo.created`（对齐 `cross-domain-messaging.md`） |
| **群内 system 文案** | 仅 im 内部 | 加群；**不对** task/bpm 开放成通知 API |

推荐：**对外业务触达只暴露 `ImBotApi`**。  
`sendSystemMessage`（或等价）仅限会话环境语义。

模块依赖约束不变：

- 同步跨域：`*-api`
- 异步跨域：领域消息
- 禁止 `*-biz → *-biz`、禁止直写他域表

---

## 11. 完整分层图

```text
┌──────────────────────────────────────────────────────────────────┐
│ L4 产品面                                                         │
│  /app/messages：direct / group / bot_dm 同一会话列表                │
│  Rail（可选）：仅聚合 bot_dm 未读 → 跳进会话                         │
│  无独立「通知中心表」作为写模型                                       │
└───────────────────────────────┬──────────────────────────────────┘
                                │ REST + WS domain=im
┌───────────────────────────────▼──────────────────────────────────┐
│ L3 IM 域（im-biz）                                                 │
│  ConversationService │ MessageService │ GroupService              │
│  BotService（目录/tenant·user 启用/ensure 会话）│ BotIngress │ BotRuntime        │
│  Presence（可并列）                                                │
└───────────────┬───────────────────────────────┬──────────────────┘
                │ 持久化                         │ im-api
                ▼                               ▼
         im_conversation                  ImBotApi
         im_message                       ImConversationApi
         im_group / im_bot…               ImMessageApi（环境文案）
                │
┌───────────────▼──────────────────────────────────────────────────┐
│ L2/L1：RealtimeEventPublisher / RealtimeTransport / WS             │
└──────────────────────────────────────────────────────────────────┘

跨域：
  system-biz ──send──► ImBotApi
  task-biz   ──send──► ImBotApi
  bpm-biz    ──send──► ImBotApi
  （禁止再以 NotifyInboxApi 作为主写路径）
```

### 11.1 三种组织方式比较（模块怎么放）

| 方案 | 做法 | 评价 |
|------|------|------|
| **A. Bot 沉到 IM（推荐）** | Bot 实体 + `ImBotApi` 均在 `module-im`；`infra` 只保留传输 | 产品叙事统一，少一个横切域 |
| **B. 通知变 Facade** | `NotifyInboxApi` 内部改调 `ImBotApi`，表逐步废弃 | 过渡期友好，防双写回潮需纪律 |
| **C. 独立 bot 模块** | `module-bot` | V1 过度拆分，不推荐 |

**推荐 A**；若迁移风险大，可短时 B，但 Facade 不得长期成为第二真源。

---

## 12. 与现状资产的映射（删 / 留 / 迁）

| 现状资产 | 目标态建议 |
|----------|------------|
| `infra_notify` + `NotifyInboxApi` + `domain=notify` 业务写 | **写路径废弃/冻结**；历史可迁成 bot_dm 或按策略丢弃 |
| Rail 铃铛 UI | **已去掉**；未读经 `/app/messages` 会话列表 `bot_dm` 角标呈现 |
| `ImMessageService.sendSystemMessage`（群加入） | **保留**，语义收窄为会话环境消息 |
| `sender_type=bot` 预留 | **落地为产品能力** |
| `im_channel` | 与通知重构无关，继续后置 |
| `ImMessageApi` / `getOrCreateDirect` 规格缺口 | 环境文案与会话 API 补齐；业务触达走 `ImBotApi` |
| 规格「通知不得写入 im_message」 | **修订**：业务触达必须写入 bot 会话消息 |
| `notify-inbox-v2` / `bpm` 的 `APPROVAL_PENDING` | 改为 Bot card + deep link |
| 空壳 `workspace-notify-system-thread` | **已删除** |

---

## 13. 平台 Bot 起步清单（V1）

经 `im_bot` 种子数据维护；成员入企自动 user enable。

| code | scope | policy | 职责 |
|------|-------|--------|------|
| `org-assistant` | tenant | default_on | 成员 / 角色 / 部门 / **成员邀请**（**type=system**） |
| `task-bot` | tenant | default_on | 到期、指派（**type=system**） |
| `approval-bot` | tenant | default_on | 待办审批（bpm）（**type=system**） |
| `account-security` | identity_fanout | mandatory | 安全、登录异常等（**type=system**） |

> 触达门禁见 [`im-bot-reach-policy-v1`](../../openspec/changes/im-bot-reach-policy-v1/)：`type=system` 免企业/用户订阅表。`invite-helper` 已退役。

外部 / 自定义 Bot：见 §7.6，V1 不实现。

---

## 14. 端到端示例

### 14.1 本企业任务到期

```text
task-biz → ImBotApi.send(
  botCode=task-bot,
  target={tenantId:A, userId:U},
  card=TASK_DUE, dedupeKey=task:{id})

→ ensure user enablement + bot_dm(A, task-bot, U)
→ insert im_message sender=bot
→ WS → U 在企业 A 的客户端
```

### 14.2 跨企业邀请（人正在 A）

```text
system-biz（B 的邀请）→ ImBotApi.send(
  botCode=org-assistant,
  target={userId:U, fanout:ALL_ACTIVE_MEMBERSHIPS},
  …)

→ 对 T_A、T_C… 各写一条 bot_dm（组织助手）
→ 用户在 A 也能看到「B 邀请了你」
```

### 14.3 群里 @自定义 Bot（将来）

```text
User @bot in group → Ingress → webhook → 外部 → 回调发回 group 消息
```

---

## 15. 对讨论建议的取舍

| 讨论点 | 判断 |
|--------|------|
| 单聊发送人/接收人清晰 | **采纳** → `direct` |
| 机器人像特殊用户 | **采纳语义**；物理上用 `im_bot`，不进 `sys_user` |
| 群是聚合空间，@Bot，无真人接收端 | **采纳** → Runtime |
| 企业订阅 vs 个人订阅混乱 | **改写**：无「个人无租户 inbox」；用 **tenant + user 双层 enablement**（`enable_policy` 分叉）+ identity fanout |
| 强制可触达何时绑定 | **采纳并具体化**：V1 系统 Bot 入企自动 user enable；发送时 ensure 会话 |
| Runtime SPI 占位 | **采纳** → V1 以 `platform` / `noop` 为主；`webhook` 仅占位（§7.6） |

---

## 16. 刻意不做（防过度设计）

- 不为 Bot 建平行 `sys_user`
- 不建无 `tenant_id` 的全局聊天库
- 不把群环境 `system` 文案与业务 Bot 推送混成一种对外 API
- V1 不做外部 Bot 安装入口、不做自定义 webhook 实装
- 不做频道（与卡片无关）
- 地基期不做交互卡片 callback **实装**（协议与分期见 §19 / [`im-bot-interactive-card.md`](im-bot-interactive-card.md)）
- Rail **不是**第二写模型
- 不为「看起来解耦」再拆独立 `module-bot`（除非后续体量证明必要）
- 平台内系统 Bot **不做**飞书式回调 URL / 验签配置面

---

## 17. 已拍板事项（2026-07-15/16）

| # | 议题 | 结论 |
|---|------|------|
| 1 | Rail 铃铛 | **去掉**；触达只走 Bot / `/app/messages` |
| 2 | Identity 扇出 | API **支持选择** SINGLE 或全 ACTIVE membership；**默认先做 SINGLE**；具体业务场景在产方切片再定 |
| 3 | 迁移姿态 | **硬切重写**：删 `infra_notify`；`0.x` 不考虑数据兼容 |
| 4 | 群内 Bot | **本版本规划并分期做成**（G0→G3，见 OpenSpec design） |
| 5 | OpenSpec 载体 | **`im-bot-notify-foundation`**；空壳已删 |
| 6 | 卡片 | **协议已定**（§19）；实装 change `im-bot-interactive-card`；平台内 SPI，非开放平台回调 URL |

---

## 18. 后续工作

执行清单见 archive [`im-bot-notify-foundation/tasks.md`](../../openspec/changes/archive/2026-07-16-im-bot-notify-foundation/tasks.md)（§7 子 change 已开单）。建议实现顺序：`im-bot-group-member` → `im-bot-runtime-platform` → `im-bot-group-mention` → **可交互卡片**（见 §19）。

---

## 19. 可交互卡片

实现约定全文：[**`im-bot-interactive-card.md`**](im-bot-interactive-card.md)。摘要：

1. **平台内系统 Bot**：发卡 `ImBotApi`；点击 `POST /app-api/im/card/action` → **进程内** `CardActionHandler`（按 `actionKey` 路由）。不配置回调 URL。
2. **Behavior**：仅 `open_url`（前端跳转）与 `callback`（可带 `form` / `formValues`）。跳转 / 一键操作 / 表单提交均覆盖。
3. **与 Bot Runtime 分开**：对话入站 ≠ 卡片动作入站。
4. **分期**：V1a 只读+跳转 → V1b callback+SPI（建议首闭环 approval-bot）→ V1c 换卡/过期/幂等 → V2 webhook。

```text
业务域 ──send(card)──► ImBotApi
用户点击 callback ──► CardActionIngress ──SPI──► bpm/task/system Handler
用户点击 open_url ──► 前端路由（不打 action API）
```

---

## 附录 A：术语表

| 术语 | 含义 |
|------|------|
| Identity | 全局账号 `sys_user` |
| Membership | 企业成员关系 `sys_tenant_user` |
| Bot Definition | 平台级机器人目录定义（不归属某个 tenant） |
| Tenant Enablement | 某企业是否允许/开通某机器人（组织策略层） |
| User Enablement | 某成员在某企业下是否授权/可用某机器人（`(tenant, user, bot)`） |
| bot_dm | 用户与机器人在某企业下的一对一会话 |
| Outbound | 业务主动经 Bot 触达用户 |
| Inbound | 用户向 Bot 发消息 / @Bot，由 Runtime 处理 |
| Card Action | 用户点击卡片按钮 / 提交表单，由 CardActionIngress 处理 |
| actionKey | 卡片 callback 的全局路由键（如 `bpm.approval.approve`） |
| Fanout | Identity 事件向多个 `(tenant, user)` 上下文投递 |
| 环境 system 消息 | 群内非真人、非业务触达的文案事件 |

## 附录 B：文档关系

| 文档 | 关系 |
|------|------|
| 本文 | IM 架构目标态（已拍板） |
| [`im-bot-interactive-card.md`](im-bot-interactive-card.md) | **可交互卡片实现约定** |
| `im-platform-foundation/design.md` | 历史双通道真源；已被本方向修订 |
| `docs/dev/product-permission-model.md` | 企业上下文 / JWT tenant 真源 |
| `docs/dev/cross-domain-messaging.md` | 同步 API vs 异步领域事件 |
| `openspec/specs/im/spec.md` | IM 行为规格（含 Bot / card 占位） |
| `openspec/specs/infra/spec.md` | infra；notify 收件箱需求已移除 |

---

## 附录 C：指正区（审阅时填写）

> 可直接在本文件追加批注，或按章节编号列出修改意见。

```text
（审阅意见写这里）

§  ：
意见：

§  ：
意见：
```
