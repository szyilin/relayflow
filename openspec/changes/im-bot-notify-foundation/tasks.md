# Tasks：im-bot-notify-foundation（母 change）

> 本文件是 **规划 + 平台地基** 执行清单。产方迁移 / 群 Bot 深化 / 可交互卡片细节以「开子 change」任务锁定顺序，避免单次会话一次做完。  
> 验证：`openspec validate im-bot-notify-foundation --strict`；含代码后 `./mvnw -pl relayflow-server -am compile`；含 `web/` 时 `pnpm build`。

## 1. 规划收口与看板

- [x] 1.1 删除空壳目录 `openspec/changes/workspace-notify-system-thread/`
- [x] 1.2 在 `docs/dev/im-messaging-architecture-draft.md` 固化已拍板结论（Rail 去掉、硬切删表、群 Bot 分期、卡片后置细节、扇出由调用方选择/默认 SINGLE），状态改为「已拍板 · 真源见本 OpenSpec」
- [x] 1.3 更新 `docs/dev/api-integration-board.md`：本地基升为优先；标注 `notify-inbox-v2` 写真源 **SUPERSEDED**；调整 V1.1 建议顺序
- [x] 1.4 更新 `AGENTS.md`「下一优先」指向 `im-bot-notify-foundation`（取代 notify 扩写优先）
- [x] 1.5 在 `openspec/changes/notify-inbox-v2/proposal.md` 顶部增加 SUPERSEDED 说明并指向本 change（未完成 notify 扩写 tasks 不再实施）
- [x] 1.6 `openspec validate im-bot-notify-foundation --strict` 通过

## 2. [平台] Schema：删 notify + Bot 表

- [ ] 2.1 新增 Flyway：`DROP TABLE` `infra_notify`（及仅服务于该表的索引/约束）；开发期不做数据迁移
- [ ] 2.2 新增 Flyway：`im_bot`、`im_bot_tenant_enablement`、`im_bot_user_enablement`；`im_conversation.type` 支持 `bot_dm`；成员主语支持 Bot（列设计见 design D2）
- [ ] 2.3 种子写入系统 Bot（至少 `org-assistant`、`invite-helper`、`task-bot`、`approval-bot`；`account-security` 可选）及默认 tenant enable 策略
- [ ] 2.4 按需 `./scripts/codegen.sh` 生成 DO/Mapper 临时参照并 diff 合入 `im-biz`（禁止从零手写 DO）

## 3. [平台] ImBotApi 与 bot_dm

- [ ] 3.1 在 `im-api` 定义 `ImBotApi` + `SendCommand`（SINGLE 必做；`ALL_ACTIVE_MEMBERSHIPS` 接口先暴露或明确二期钩子，见 design D4）
- [ ] 3.2 实现 ensure bot_dm、user enablement 校验、消息落库 `sender_type=bot`、`dedupeKey` 幂等
- [ ] 3.3 落库后经 `RealtimeTransportApi` 推送 `domain=im, type=message.new`（仅 User 成员）
- [ ] 3.4 入企 ACTIVE 钩子：为 mandatory/default_on 系统 Bot 自动写 user enablement
- [ ] 3.5 会话列表 API 返回 `bot_dm`（预览/未读字段与现网一致）
- [ ] 3.6 收窄并文档化 `sendSystemMessage`：仅环境文案；业务禁止走该入口
- [ ] 3.7 `./mvnw -pl relayflow-server -am compile`（或相关模块测试）通过

## 4. [平台] 拆除 Notify 写栈与 REST

- [ ] 4.1 删除 `NotifyInboxApi` 及 infra-biz 实现、Mapper/DO/`infra_notify` 残留引用
- [ ] 4.2 删除 `/app-api/infra/notify/*` Controller 与相关 VO
- [ ] 4.3 清理 `domain=notify` 业务 Handler / Publisher 义务（枚举可暂留 no-op，不得再写真源）
- [ ] 4.4 临时断开 `system-biz` / `task-biz` 对 `NotifyInboxApi` 的编译依赖（可先去掉 push 调用，产方改道见 §7 子 change；保证 server 可编译启动）

## 5. 前端：去掉 Rail 铃铛

- [ ] 5.1 移除工作台 Rail 通知铃铛入口、notify store 业务拉取与 `notify.new` 订阅
- [ ] 5.2 清理 `/app-api/infra/notify` 前端 API 客户端；注册页 pending invite banner **保留**
- [ ] 5.3 `cd web && pnpm build` 通过
- [ ] 5.4 浏览器冒烟：登录工作台无铃铛；`/app/messages` 人聊仍可用

## 6. 前端：bot_dm 可见（可与 §3 联调或开子 change `im-bot-dm-web`）

- [ ] 6.1 `/app/messages` 会话列表展示 `bot_dm`（名称/头像用 Bot 元数据）
- [ ] 6.2 打开 bot_dm 可拉历史；未读角标走会话未读（无独立铃铛）
- [ ] 6.3 契约写入 `openspec/lanes/`（新建 lane 或扩展 im 既有 contract）
- [ ] 6.4 `pnpm build` + 浏览器路径验证（可用临时管理调 `ImBotApi` 或后续 invite 切片造数）

## 7. 后续子 change（本母 change 勾选表示「已开单/已拆分」，非一次实现）

- [ ] 7.1 开 `im-bot-invite-migrate`：`MEMBER_INVITE` → `invite-helper` + 选定 SINGLE/fanout
- [ ] 7.2 开 `im-bot-task-due-migrate`：`TASK_DUE` → `task-bot`（承接并替代 notify-inbox-v2 产方 tasks）
- [ ] 7.3 修订 `bpm-v1`：触达改为 `approval-bot` + `ImBotApi`，去掉对 `infra_notify` 类型目录依赖
- [ ] 7.4 开 `im-bot-group-member`：群挂载/移除 Bot（G1）
- [ ] 7.5 开 `im-bot-group-mention`：@Bot → Ingress（G2）
- [ ] 7.6 开 `im-bot-runtime-platform`：Runtime SPI + platform/noop（G3）
- [ ] 7.7 开 `im-bot-interactive-card`：飞书向可交互卡片 + callback（细节届时定）

## 8. 卡片占位（地基内最小）

- [ ] 8.1 在消息 content 契约/枚举预留 `card`（及 actions 注释）；实现允许 text + deep link 发送
- [ ] 8.2 文档注明交互 callback **不做**于地基；禁止回潮 notify

## 9. 收尾

- [ ] 9.1 同步修订 `docs/dev/` 中仍写「通知不得写入 im_message」或 Rail 铃铛为写真源的过时描述
- [ ] 9.2 归档本 change 前：`openspec` 主规格同步（im/infra/system/task/web-auth）
- [ ] 9.3 处理 `notify-inbox-v2`：abort archive 或文档化废弃后移入 archive
