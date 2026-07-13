# Tasks：notify-inbox-v2（母 change · 执行路线图）

> **用法**：本文件是通知中心 V2 的 **总路线图**。实际编码按 **子切片** 分批执行；每次会话只做一组 task（建议 ≤10 条 checkbox）。  
> **顺序**：`[平台]` 可先行；业务 UI 默认 **前端优先**（`-web` → `-api` → `-integrate`）。  
> **设计真源**：`design.md`；**行为**：`specs/infra`、`specs/task`。

---

## 0. 规划基线（本 change）

- [x] 0.1 `proposal.md` / `design.md` / spec delta / 本 `tasks.md`
- [x] 0.2 `openspec validate notify-inbox-v2 --strict`
- [x] 0.3 更新 `docs/dev/api-integration-board.md` 登记 `notify-inbox-v2` 切片（planned）
- [x] 0.4 `specs-sync-checklist.md`（归档前核对清单）

---

## 1. [平台] notify-inbox-v2-schema

**目标**：`dedupe_key` 列、类型常量、`NotifyItemCommand` 扩展、幂等逻辑。  
**范围**：Java + Flyway；无 `web/`。

- [x] 1.1 Flyway `V0.1.0.{n}__infra_notify_dedupe_key.sql`（可空列 + 部分索引）
- [x] 1.2 扩展 `InfraNotifyType`：`TASK_DUE` / `TASK_ASSIGNED` / `IM_MENTION` / `APPROVAL_PENDING`
- [x] 1.3 `NotifyItemCommand` + DO/Mapper 支持 `dedupeKey`（codegen 或手工合并 DO 字段）
- [x] 1.4 `NotifyInboxServiceImpl`：有 `dedupeKey` 时按 tenant+receiver+type+key 未读幂等更新
- [x] 1.5 单元测试：dedupe 刷新、无 key 时邀请行为不变
- [x] 1.6 `./mvnw -pl relayflow-server -am compile`（含相关 test）

**验证**：单测 + compile。

---

## 2. [平台] notify-inbox-v2-realtime

**目标**：`push` 后对在线 `userId` 下发 `domain=notify` / `type=notify.new`。  
**依赖**：§1（可与 §1 同会话若体量允许）。

- [x] 2.1 `RealtimeEventPublisherImpl`：`NOTIFY` 走 `sendToUsers`（与 SYSTEM 同源）
- [x] 2.2 `NotifyInboxServiceImpl.push` 成功后 publish（payload 含至少 `unreadCount`）
- [x] 2.3 `userId == null` 不发 WS；单测/日志覆盖
- [x] 2.4 `./mvnw -pl relayflow-module-infra-biz -am test` 或 server compile

**验证**：单元/集成测试或本地双客户端冒烟说明。

---

## 3. notify-inbox-v2-api

**目标**：page 按 type 筛选、`read-all`；可选 unread `byType`。

- [x] 3.1 `GET /app-api/infra/notify/page?type=` 过滤实现 + VO 透出
- [x] 3.2 `POST /app-api/infra/notify/read-all`（可选 body `type`）
- [x] 3.3 （可选）`unread-count` 增加 `byType` map
- [x] 3.4 Security：仅当前用户数据；curl/单测验收
- [x] 3.5 `./mvnw -pl relayflow-server -am compile`

**完成后**：看板 api → `ready`。

---

## 4. notify-task-due-api

**目标**：task-biz 到期生产方。  
**依赖**：§1（NotifyInboxApi + types）。

- [x] 4.1 `task-biz` pom 依赖 `infra-api`（若尚未）
- [x] 4.2 配置 `relayflow.task.due-remind-window`（默认 24h）
- [x] 4.3 create/update：窗口内 TODO → `NotifyInboxApi.push(TASK_DUE)` + `dedupeKey=task:{id}` + payload.route
- [x] 4.4 `pageMyTasks` lazy 补偿：窗口内缺未读 `TASK_DUE` 则补 push
- [x] 4.5 单测：窗口内/外/幂等；禁止依赖 infra-biz
- [x] 4.6 `./mvnw -pl relayflow-module-task-biz -am test` 或 server compile

---

## 5. notify-inbox-v2-web（前端 lane）

**目标**：多类型铃铛 UI + deep link + contract；可先 Mock 新 API。  
**依赖**：contract 可先于后端；联调依赖 §2–§4。

- [x] 5.1 起草 `openspec/lanes/notify-inbox-v2/contract.md`（page filter、read-all、WS envelope、TASK_DUE 形状）
- [x] 5.2 扩展 `api/app/notify.ts`、`stores/notify.ts`：type 筛选、read-all
- [x] 5.3 `WorkspaceNotifyBell`：类型图标、筛选 Chip、全部已读、点击 `payload.route` 跳转
- [x] 5.4 接入现有 WS：监听 `notify` / `notify.new` 刷新角标（可抽公共 realtime 监听）
- [x] 5.5 空状态与 `MEMBER_INVITE` 说明文案更新
- [x] 5.6 `cd web && pnpm build`

**验证**：`pnpm build`；浏览器路径写入 contract。

**完成后**：看板 web → `ui_ready`。

---

## 6. notify-inbox-v2-integrate（联调）

- [x] 6.1 前端接真实 filter / read-all / WS（去 Mock）
- [ ] 6.2 E2E：创建 1h 内到期任务 → 铃铛 `TASK_DUE` → 点击进 `/app/tasks`
- [ ] 6.3 E2E：已登录用户收到邀请时角标 WS 刷新（若可双会话）
- [x] 6.4 `openspec validate notify-inbox-v2 --strict`
- [x] 6.5 `./mvnw -pl relayflow-server -am compile` + `cd web && pnpm build`
- [ ] 6.6 看板 `notify-inbox-v2` → **done**

---

## 7. 母 change 归档前

- [ ] 7.1 确认 specs delta 与实现一致
- [ ] 7.2 `openspec archive notify-inbox-v2`（同步 main specs；按 CLI 流程）
- [ ] 7.3 `./mvnw verify`（如适用）+ `cd web && pnpm build`

---

## 执行顺序速查

```text
Session 1   §0 校验 + 看板 + §1 schema
Session 2   §2 realtime + §3 api
Session 3   §5 web（可与 Session 2 并行若 contract 已定）
Session 4   §4 task-due-api
Session 5   §6 integrate + §7 归档
```

推荐并行：§5-web 与 §3-api 在 contract 冻结后按 parallel-lane 拆分。

## 后续 change（不在本路线图）

| Change | 说明 |
|--------|------|
| `workspace-tasks-assign` | 指派 → `TASK_ASSIGNED` |
| `im-mention-*` | @我 → `IM_MENTION` |
| `bpm-*` | 审批 → `APPROVAL_PENDING` |
| `workspace-search-*` | 全局搜索 |
| `notify-inbox-page` | 独立通知中心页 |

---

## 会话开场白模板

```text
Using change: notify-inbox-v2（§1 schema / 或 §5 web）
Read: openspec/changes/notify-inbox-v2/design.md
Tasks: openspec/changes/notify-inbox-v2/tasks.md 对应章节
```
