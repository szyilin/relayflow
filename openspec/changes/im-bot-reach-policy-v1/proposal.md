## Why

当前 Bot 触达要求 **企业订阅 + 用户订阅同时满足**，且入企时把企业侧能力「拷贝」到用户关联表。这导致：新企业无种子订阅行就发不出消息（邀请失败弹「机器人未启用」）；用户退企还要清理个人订阅；系统内置助手本应人人可达却被订阅表卡住。业务产方（邀请）与触达耦合后，触达失败还会 **阻断主业务并暴露给前端**。

## What Changes

- **BREAKING（触达判定）**：`ImBotApi.send` 可达性改为：
  1. **系统 Bot**（`im_bot.type=system`）：任意企业、任意 ACTIVE 用户均可投递，**不查**企业/用户订阅表
  2. **非系统 Bot**：企业订阅 **或** 用户订阅任一存在即可（**并集**）；不再要求两层都写、也不再入企自动把企业订阅拷贝到用户表
- `im_bot` 增加 `type`（或等价分类字段）：至少区分 `system` / `tenant`（可安装类后续扩展）
- 种子 Bot（组织助手、任务助手、审批助手、账号安全等）标为 `system`
- **弱化/停用**「入企强制写 `im_bot_user_enablement`」对 system Bot 的义务；用户表仅服务 opt-in / 个人订阅场景
- **产方隔离**：邀请等业务调用 `ImBotApi.send` 失败时 **不得** 回滚主事务、**不得** 以触达错误码返回前端（记录日志，主流程成功）
- 修订架构草案与 foundation 中「须双层 enablement」的过时描述

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `im`：Bot 分类与可达性判定（system 免订阅；非 system 并集）；发送失败语义对调用方透明化约定
- `system`：邀请触达失败不阻断邀请 API 成功响应

## Impact

- DB：Flyway 为 `im_bot` 增加 `type`；种子更新；可选清理无用的强制 user enablement 路径
- `im-biz`：`ImBotServiceImpl.deliverInTenant` 判定逻辑
- `system-biz`：`inviteMember` / 其它 `ImBotApi.send` 调用改为 catch + log（或 API 明确「best-effort」）
- 文档：`docs/dev/im-messaging-architecture-draft.md`、母 change / invite-migrate 相关表述
- 前端：邀请页不再因 Bot 未启用报错（后端修好后自然消失）
