# 提案：IM Bot 统一业务触达地基（im-bot-notify-foundation · 母 change）

## Why

当前「人聊走 `im_*`、业务提醒走 `infra_notify` + Rail 铃铛」双通道，与飞书「业务触达 = 机器人会话消息」不一致，也阻塞跨域卡片/审批/任务提醒进入同一会话列表。架构结论已收口于 [`docs/dev/im-messaging-architecture-draft.md`](../../../docs/dev/im-messaging-architecture-draft.md)；本 change 把该草案升格为 **可执行 OpenSpec 真源**，作为后续 Bot 能力、产方迁移与前端改造的**前提地基**。

开发期产品版本为 `0.x`，**允许推倒重来**（含删表）；不保留 `infra_notify` 双写/兼容层。

## What Changes

本 change 为 **母 change（规划 + 规格纠偏 + 平台地基）**；具体能力按纵向切片拆后续子 change（见 design / tasks）。本 change 自身交付以 **规格修订、schema、跨域 `ImBotApi`、去 Rail/去 notify 写真源** 为主，并规划群内 Bot 与可交互卡片的分期实现。

1. **统一写真源**：**BREAKING** 业务触达只写入 `im_message`（`sender_type=bot`，会话 `bot_dm` 或群内 Bot）；废弃以 `infra_notify` 为真源的写路径
2. **去掉 Rail 铃铛**：**BREAKING** 前端不再提供独立通知铃铛；触达一律出现在 `/app/messages`（bot 会话 / 群）
3. **删除 Notify Inbox 栈**：**BREAKING** 删除 `infra_notify` 表、`NotifyInboxApi` 业务写入口、`domain=notify` 业务语义及对应 REST/UI（历史数据不迁移）
4. **Bot 模型落地**：平台级 `im_bot` 目录 + tenant/user enablement + 会话 ensure；V1 系统 Bot 入企自动 user enable
5. **跨域唯一入口**：`ImBotApi.send`（目标默认单 `(tenantId, userId)`；可选 fanout 全部 ACTIVE membership——由调用方按场景选择，**不**在地基期锁死各业务策略）
6. **群内 Bot（本版本规划、分步实现）**：群可挂 Bot 成员；用户 @Bot → Ingress → Runtime；本期先定契约与分步切片，实装可后于 bot_dm Outbound
7. **可交互卡片（规划占位）**：`content` 类型预留交互 card + callback 契约；**本地基不实现**交互细节，后续独立切片细化
8. **规格纠偏**：修订 `im`「通知不得写入 `im_message`」与 `infra` Notify 相关需求；废止空壳 `workspace-notify-system-thread`；**冻结并改道**进行中的 `notify-inbox-v2` / 依赖 notify 类型目录的 `bpm-v1` 触达假设
9. **产方迁移（后续子切片）**：`MEMBER_INVITE` / `TASK_DUE` / 规划中审批等改调 `ImBotApi`（种子 Bot：`org-assistant` / `invite-helper` / `task-bot` / `approval-bot` 等）

## Capabilities

### New Capabilities

- （无独立新 domain 目录；Bot 归属 `im`，传输仍归 `infra`）

### Modified Capabilities

- `im`：增加 bot_dm / Bot 目录与 enablement / `ImBotApi` / Bot Ingress·Runtime；业务触达必须写 `im_message`；群内 Bot 需求纳入本版本规划；废止「禁止通知写入 im_message」条款；收窄 system 消息为会话环境文案
- `infra`：**BREAKING** 删除 Notify Inbox 写真源与 `NotifyInboxApi` 业务义务；`domain=notify` 不再作为业务触达通道；Realtime 仅保留传输（IM 等仍经 `RealtimeTransportApi`）
- `system`：成员邀请等触达改为 `ImBotApi`（本母 change 定契约；实现落后续切片）
- `task`：到期等触达改为 `ImBotApi`（同上）
- `web-auth`：**BREAKING** 去掉工作台 Rail 通知铃铛入口；触达改为在 `/app/messages` 的 Bot 会话中呈现
- （衔接）`bpm-v1`：触达假设从 `APPROVAL_PENDING`→notify 改为 approval-bot→`ImBotApi`（在 bpm change 内修订，本 change 写明依赖关系）

## Impact

| 层 | 模块/路径 | 变更 |
|----|-----------|------|
| 文档 | `docs/dev/im-messaging-architecture-draft.md`、对接看板、AGENTS 优先序 | 草案拍板结论固化；看板去 Rail/notify 路线 |
| 规格 | `openspec/specs/im`、`infra`（归档时同步）；`task`/`system` delta | 写真源与跨域 API 纠偏 |
| DB | Flyway | **DROP** `infra_notify`；新增 `im_bot` / enablement；`im_conversation.type` 含 `bot_dm`；成员可挂 bot subject |
| 后端 | `relayflow-module-im-api/biz` | Bot 服务、`ImBotApi`、Ingress/Runtime SPI、种子 Bot |
| 后端 | `relayflow-module-infra-api/biz` | 删除 NotifyInbox 实现与 app notify REST；清理 WS notify 业务 |
| 后端 | `system-biz` / `task-biz` | 后续切片改调 `ImBotApi` |
| 前端 | `web/` | **移除** Rail 铃铛与 notify store 业务；`/app/messages` 纳入 bot_dm（及后续群 Bot） |
| 进行中 change | `notify-inbox-v2`、空壳 `workspace-notify-system-thread` | 空壳删除；notify-inbox-v2 **停止扩写真源**，改道/归档策略见 design |

**回滚 / 迁移（自部署）**：开发期 `0.x` **不做**数据兼容与双写；删表后旧通知不可恢复。回滚仅能靠发布前 Git/镜像回退，不做 forward-fix 脚本。

## 非目标（本母 change / 近期切片明确不做）

- 外部可安装 Bot、webhook 实装（schema/枚举可占位）
- 频道（channel）产品化
- 可交互卡片的 callback 完整产品（仅规划占位）
- 独立 `module-bot`
- 无 `tenant_id` 的全局账号 inbox
- 为 Bot 创建平行 `sys_user`
- Presence WS 推送、mute/pin 等与 Bot 重构无关的 IM 增强

## 已拍板结论（产品 / 工程）

| # | 议题 | 结论 |
|---|------|------|
| 1 | Rail 铃铛 | **去掉**；触达只走 Bot / 会话列表 |
| 2 | Identity 扇出 | **API 支持选择范围**；默认实现先支持「单 `(tenant,user)`」；全 membership fanout 为可选能力，**按业务场景调用时再定**，地基不锁死策略表 |
| 3 | 迁移姿态 | **硬切重写**：删 `infra_notify` 与 Notify 写栈；`0.x` 不考虑数据兼容 |
| 4 | 群内 Bot | **本版本规划并分期做成**（细节工程拆步） |
| 5 | OpenSpec 载体 | **本 change**；删除空壳 `workspace-notify-system-thread` |
| 6 | 卡片 | **规划可交互 card**（飞书向）；细节后续切片再定 |

## 参考

- 架构草案：[`docs/dev/im-messaging-architecture-draft.md`](../../../docs/dev/im-messaging-architecture-draft.md)
- 历史双通道：[`archive/.../im-platform-foundation/design.md`](../archive/2026-07-12-im-platform-foundation/design.md)
- 跨域约定：[`docs/dev/cross-domain-messaging.md`](../../../docs/dev/cross-domain-messaging.md)
- 版本策略：[`CONTRIBUTING.md`](../../../CONTRIBUTING.md)（开发期 `0.x`）
