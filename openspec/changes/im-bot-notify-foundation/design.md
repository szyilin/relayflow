# 设计：IM Bot 统一业务触达地基

## Context

- **as-is**：人聊/群聊 MVP（`im_*` + WS `domain=im`）已上线；业务提醒走 `infra_notify` + Rail 铃铛（`NotifyInboxApi`、`domain=notify`）。
- **目标态草案**：[`docs/dev/im-messaging-architecture-draft.md`](../../../docs/dev/im-messaging-architecture-draft.md)（已拍板，见 proposal）。
- **约束**：跨域仅 `*-api`；IM 推送经 `RealtimeTransportApi`；开发期 `0.x` 允许删表硬切；前端优先纵向切片推进用户可见能力。
- **并行债**：`notify-inbox-v2` 仍按旧写真源立项；本地基 supersede 其写真源方向。空壳 `workspace-notify-system-thread` 删除。

## Goals / Non-Goals

**Goals:**

1. 规格真源：业务触达 = Bot 发 `im_message`；删除 Notify Inbox 义务。
2. 平台地基：Bot 目录 / enablement / `bot_dm` ensure / `ImBotApi.send`（含可选 fanout）。
3. 去掉 Rail 铃铛；会话列表成为触达唯一产品入口。
4. 群内 Bot：本版本规划完整契约，分步落地（Outbound bot_dm → 群挂载 → @ Ingress → Runtime）。
5. 可交互卡片：content 契约占位，Callback 后置切片。
6. 给出后续产方迁移与前端切片路线，供子 change 执行。

**Non-Goals:**

- 本母 change 一次实现全部产方迁移、完整交互卡片、外部 webhook Bot。
- 频道产品化、独立 `module-bot`、无 tenant 全局 inbox。
- 为 `notify-inbox-v2` 做数据迁移或 Facade。

## Decisions

### D1 — 模块归属：Bot 沉在 `module-im`（方案 A）

| 选项 | 结论 |
|------|------|
| A. Bot 在 im | **采纳**：目录、enablement、`ImBotApi`、Ingress/Runtime 均在 im |
| B. Notify Facade | **不采纳**：硬切删表，不做长期 Facade |
| C. 独立 module-bot | **不采纳**：V1 过度拆分 |

`infra` 只保留 WS/Realtime 传输；不再持有业务触达写真源。

### D2 — 会话类型与成员主语

- `im_conversation.type` 增加 **`bot_dm`**（User↔Bot 一对一，unique `(tenant_id, bot_id, user_id)` 语义通过成员 + 类型保证）。
- 成员主语扩展：`subject_type=user|bot`（实现上可在 `im_conversation_member` 增加列，或等价映射表；**禁止**把 Bot 写入 `sys_user`）。
- `direct` / `group` 保持现状；`channel` 继续后置。
- `sender_type`：`user` | `bot` | `system`（system **仅**会话环境文案，如加群）。

### D3 — Enablement（V1 系统 Bot）

```text
im_bot                    # 平台目录（无 tenant）
im_bot_tenant_enablement  # (tenant_id, bot_id)
im_bot_user_enablement    # (tenant_id, user_id, bot_id)
```

- 策略：`mandatory` / `default_on`（`opt_in` / `installable` 仅枚举占位）。
- 入企 ACTIVE → 为已 tenant-enable 的系统 Bot 自动写 user enable；**不**预建空会话。
- 首次 `ImBotApi.send` / 用户打开 Bot / 群 @Bot → ensure 会话。

种子 Bot（可随实现微调文案，code 稳定）：

| code | 职责 |
|------|------|
| `org-assistant` | 组织/角色/**成员邀请**等 |
| `invite-helper` | **已退役**（邀请归 `org-assistant`，见 `im-bot-invite-migrate`） |
| `task-bot` | 任务到期/指派 |
| `approval-bot` | 审批待办 |
| `account-security` | 安全类（可选首批种子） |

### D4 — `ImBotApi.send` 与投递范围

```text
ImBotApi.send(SendCommand):
  botCode, content, dedupeKey?
  target:
    - SINGLE { tenantId, userId }          # 默认、首期必做
    - ALL_ACTIVE_MEMBERSHIPS { userId }    # 可选能力；调用方按场景选用
```

- 幂等：`(tenant_id, bot_id, user_id, dedupe_key)` 在约定窗口内唯一（承接原 `dedupe_key` 思路）。
- **不**在地基期规定「邀请必须 fanout」——调用方显式选 target；默认文档示例用 SINGLE。

### D5 — 硬切删除 Notify 栈

删除/停止：

- 表 `infra_notify`（Flyway DROP，无数据迁移）
- `NotifyInboxApi` 及 infra-biz 实现、app `/app-api/infra/notify/*`
- 前端 Rail 铃铛、notify store、`domain=notify` 业务订阅
- 规格中「通知不得写入 im_message」条款

`RealtimeEventPublisher` 对 NOTIFY：改为无业务义务（可删枚举或保留 no-op 但不准再作写真源）。

### D6 — 群内 Bot 分期

| 步 | 内容 |
|----|------|
| G0 | 成员模型允许 Bot；群创建/邀请 API **暂不**暴露挂 Bot（schema 就绪） |
| G1 | 管理/API：群添加/移除 Bot 成员；列表展示 Bot |
| G2 | 用户消息含 `@bot` / mention 块 → Ingress |
| G3 | Runtime：`platform` / `noop` handler；回复进同群；`webhook` 仅占位 |

Outbound 业务触达 **不依赖** G1–G3，可先靠 bot_dm 上线。

### D7 — 可交互卡片占位

- `im_message.type` / content blocks 预留 `card`（及后续 `actions`）。
- 契约文档注明：飞书向可交互 card + callback **后续切片**细化（鉴权、超时、幂等）。
- 地基期允许产方先发 **text + deep link**（`route` / `entityType` / `entityId`），不阻塞迁移。

### D8 — 与进行中 OpenSpec 的关系

| Change | 处置 |
|--------|------|
| `workspace-notify-system-thread` | **删除**目录 |
| `notify-inbox-v2` | **停止**按 `infra_notify` 扩写；已完成部分在硬切切片中拆除；未完成 tasks 标记废弃或改道到本地基后续切片 |
| `bpm-v1` | 修订触达：`approval-bot` + `ImBotApi`，去掉对 notify 类型目录的硬依赖 |
| `workspace-search-v1` | 可保留；搜索结果不再依赖 notify 实体 |

### D9 — 实施分层（母 change vs 子切片）

**本母 change 建议直接落地（[平台] 允许无 UI 先行）**：

1. 规格 delta（本目录）
2. Flyway：DROP notify + Bot schema + seed
3. `ImBotApi` + ensure + 单 tenant send + WS `domain=im`
4. 拆除 notify API/表/前端铃铛（可与 schema 同一切片或紧随）

**后续子 change（前端优先，示例命名）**：

```text
im-bot-schema-api          # 若母 change 只落规格，则 schema+api 独立；推荐母 change 含平台地基 tasks
im-bot-dm-web              # 会话列表 bot_dm UI
im-bot-invite-migrate      # system 邀请 → org-assistant（废弃 invite-helper）
im-bot-task-due-migrate    # task 到期 → task-bot
im-bot-group-member        # 群挂 Bot
im-bot-group-mention       # @Bot Ingress
im-bot-runtime-platform    # Runtime SPI + noop/platform
im-bot-interactive-card    # 可交互卡片细节
```

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 硬切后本地/演示环境通知历史消失 | 接受；文档写明；compose 重新 migrate |
| `notify-inbox-v2` 半成品与地基冲突 | 看板与 AGENTS 改序；先拆 notify 再启 Bot 产方 |
| 群 Bot 范围诱使 scope 膨胀 | 强制 G0→G3 顺序；Outbound 不阻塞 |
| 无铃铛导致「找不到提醒」 | `/app/messages` 突出 bot 会话未读；注册页 pending invite banner **保留**（非铃铛） |
| fanout 能力滥用 | API 显式 target；产方 code review / 规格场景约束 |

## Migration Plan

1. 合并规格 delta → 实现平台地基 + DROP `infra_notify`。
2. 同窗口拆除前端铃铛与 notify 客户端。
3. 逐个迁移产方（invite → task due → bpm）。
4. 群 Bot 按 G1–G3；卡片独立切片。
5. Archive 本母 change 时同步 `openspec/specs/*`；归档或关闭 `notify-inbox-v2`。

**回滚**：仅 Git/镜像回退；无 forward 数据恢复。

## Open Questions

| # | 状态 | 说明 |
|---|------|------|
| 邀请默认 SINGLE 还是 fanout | **开放给实现切片** | API 两种都支持；invite 切片拍板具体 target |
| `im_conversation_member` 是否加 `subject_type` 列 vs 旁表 | 实现时选 | 倾向成员表加列，简单 |
| 种子 Bot 是否首批含 `account-security` | 可选 | 无生产方前可只 seed、noop inbound |
| `notify-inbox-v2` 归档方式（abort archive vs 改道 document） | 需负责人一句 | 推荐：active 目录加 SUPERSEDED 说明后 archive 为 aborted |

## 参考

- 草案全文与分层图：`docs/dev/im-messaging-architecture-draft.md`
- 跨域：`docs/dev/cross-domain-messaging.md`
- 已拍板结论：本 change `proposal.md` §已拍板结论
