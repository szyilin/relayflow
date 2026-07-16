# Tasks：im-bot-reach-policy-v1

> 验证：`openspec validate im-bot-reach-policy-v1 --strict`；`./mvnw -pl relayflow-server -am compile`；含产方测试；邀请回归（新企业 + ACTIVE 用户）不报「机器人未启用」。

## 1. 规格与文档

- [x] 1.1 修订 `docs/dev/im-messaging-architecture-draft.md` §7：system 免订阅；非 system 并集；删除「入企必须写 user enable」对系统 Bot 的硬性要求
- [x] 1.2 看板/母 change 交叉引用本 change（可选一句）
- [x] 1.3 `openspec validate im-bot-reach-policy-v1 --strict`

## 2. Schema

- [x] 2.1 Flyway：`im_bot` 增加 `type`（`system` | `tenant`），CHECK 约束；已有种子 Bot 回填 `system`
- [x] 2.2 DO/Mapper 合入 `type` 字段（codegen 或最小 diff）

## 3. 发送判定

- [x] 3.1 `ImBotServiceImpl`：`system` 跳过 tenant/user require；非 system 改为并集（tenant **或** user）
- [x] 3.2 `ensureUserEnablementsOnActive`：对 `type=system` 不再写 user enablement（或整体 no-op for system）
- [x] 3.3 单元/编译验证发送路径

## 4. 产方 best-effort

- [x] 4.1 `UserServiceImpl.pushMemberInviteBotMessage`（及同类 send）：catch 触达异常，打 warn 日志，不抛出；`send` 使用 `REQUIRES_NEW` 避免污染邀请事务
- [x] 4.2 更新 `UserServiceImplInviteTest`：mock send 抛错时 invite 仍成功
- [x] 4.3 确认其它已接线的 `ImBotApi.send` 产方同样 best-effort（若无则注明）— 目前仅邀请产方调用 send

## 5. 验证

- [x] 5.1 `./mvnw … UserServiceImplInviteTest` + `relayflow-server` compile 通过
- [ ] 5.2 浏览器/API：新注册企业邀请已有 ACTIVE 用户 → 邀请成功无红 toast；有 ACTIVE 时组织助手可收消息
