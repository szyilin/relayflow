# Tasks：workspace-calendar-v1（母 change · 执行路线图）

> **用法**：本文件是工作台日历的 **总路线图**。实际编码按 **子 change** 分批执行；每次会话只做一个子 change 内的一组 task（通常 ≤10 条）。  
> **顺序**：默认 **前端优先**（`-web` → `-api` → `-integrate`）；`[平台]` 可先行。  
> **拍板**：邀约要；整日历共享 V1.1；无任务图层；无 RRULE；设置进全局设置窗 preference。

---

## 0. 规划基线（本 change）

- [x] 0.1 `proposal.md` / `design.md` / spec deltas（`calendar`、`user-preference`、`im`）/ 本 `tasks.md`
- [x] 0.2 `openspec validate workspace-calendar-v1 --strict`
- [x] 0.3 更新 `docs/dev/api-integration-board.md` 登记 `workspace-calendar`（planned）与建议切片顺序
- [x] 0.4 `docs/dev/database.md` 表前缀表增补 `cal_`（并可注明已有 `task_`）

---

## 1. [平台] calendar-schema-v1

**目标**：`cal_*` 表 + Maven 模块 + `calendar-bot` 种子 + server 可加载。  
**范围**：Java / Flyway；无 `web/` 业务页。

- [x] 1.1 根 `pom.xml` + `relayflow-module-calendar`（api + biz）脚手架（复制 `task` 模块模式）
- [x] 1.2 Flyway：`cal_calendar` / `cal_event` / `cal_attendee`；种子 `im_bot` 中 `calendar-bot`（`type=system`）
- [x] 1.3 `./scripts/codegen.sh --module calendar --tables cal_calendar,cal_event,cal_attendee` → diff 合入 `src/`
- [x] 1.4 `relayflow-server/pom.xml` 引入 `relayflow-module-calendar-biz`；模块可启动
- [x] 1.5 `./mvnw -pl relayflow-server -am compile`

**验证**：compile + 迁移成功。

**完成后**：可开 `workspace-calendar-web`（或与 web 并行，但 contract 以 web 为先起草）。

---

## 2. workspace-calendar-web（前端 lane · 第一步）

- [x] 2.1 起草 `openspec/lanes/workspace-calendar/contract.md`（日历/日程/响应/偏好键）
- [x] 2.2 `api/app/calendar.ts` + `stores/calendar.ts`（临时数据仅 store；禁止常驻 `mocks/`）
- [x] 2.3 Rail + 路由 `/app/calendar`：侧栏迷你月历 + 我管理的日历（勾选/色）+ 添加日历；**无**「我的任务」图层
- [x] 2.4 主区：日/周/月切换、今天/翻页、当前时间线（日/周）、快捷创建弹层、事件块按日历着色
- [x] 2.5 完整编辑：参与人选择（通讯录成员）、提醒；点击事件查看/编辑/删除（按角色禁用）
- [x] 2.6 设置窗：左栏新增「日历」分类，绑定 preference 形状（可先本地合并默认键）
- [x] 2.7 `cd web && pnpm build && pnpm typecheck`
- [x] 2.8 浏览器：`/app/calendar` 建日历/建日程/切视图；设置窗改周起始

**验证**：`pnpm build` + `pnpm typecheck` + 浏览器路径。

**完成后**：看板 web → `ui_ready`；可开 `-api`。

---

## 3. workspace-calendar-api（后端 lane）

**依赖**：`calendar-schema-v1` 完成；`workspace-calendar-web` contract 就绪。

- [x] 3.1 PRIMARY ensure（A 类）+ calendar list/create/update/delete（主日历不可删；非空 OWNED 拒删）
- [x] 3.2 Event CRUD + 区间 list（自有日历 ∪ 作为 attendee）+ get
- [x] 3.3 Attendee 邀请（system-api 校验成员）+ respond accept/decline
- [x] 3.4 `ImBotApi`：邀约/变更/取消/提醒（`calendar-bot`，dedupe，best-effort）
- [x] 3.5 user-preference 代码默认合并 `settings.calendar` 键
- [x] 3.6 Security：`/app-api/calendar/**` JWT + 有效成员；错误码按 contract
- [x] 3.7 单测或 curl（见 contract）+ `./mvnw -pl relayflow-server -am compile`

**完成后**：看板 api → `ready`；开 `-integrate`。

---

## 4. workspace-calendar-integrate（联调）

- [x] 4.1 `stores/calendar.ts` 去临时数据；设置窗日历分类走 preference API
- [x] 4.2 E2E：建日历 → 建日程 → 邀请同事 → 对方可见/可响应 → 提醒或邀约出现在消息 bot_dm
- [x] 4.3 深链 `/app/calendar?eventId=` 打开对应日程
- [x] 4.4 `openspec validate workspace-calendar-v1 --strict`
- [x] 4.5 `./mvnw -pl relayflow-server -am compile` + `cd web && pnpm build && pnpm typecheck`
- [x] 4.6 看板 `workspace-calendar` → **done**

**验证说明**：4.2 已用 curl 双账号验证（建日历/日程 → 邀请 → 对方 get/list → respond ACCEPTED → `calendar-bot` bot_dm）。联调中修复：`im_message.client_msg_id` 超长导致 Bot 通知失败（ImBot 对超长 dedupe 做 SHA-256 截断 + 日历短 key）。

---

## 5. 母 change 归档前

- [x] 5.1 全部子 change archive（若已拆独立目录）— 未拆独立目录，跳过
- [x] 5.2 `openspec archive workspace-calendar-v1`（同步 `openspec/specs/calendar` 等）
- [x] 5.3 确认 `database.md` / board / `workspace-ui-patterns` 日历页摘要已更新

---

## 执行顺序速查

```text
Session 0   §0 规划基线（validate + board + database 前缀）
Session 1   §1 calendar-schema-v1
Session 2   §2 workspace-calendar-web
Session 3   §3 workspace-calendar-api
Session 4   §4 integrate + §5 归档
```

## 后续 change（不在本路线图）

| Change | 说明 |
|--------|------|
| `workspace-calendar-share` | 整日历共享 / 订阅同事 / busy-free |
| `workspace-calendar-rrule` | 重复日程与例外 |
| `workspace-calendar-dnd` | 拖拽改期改时长 |
| 会议室 / CalDAV / 公共日历 | 另立项 |

---

## 会话开场白模板

```text
Using change: workspace-calendar-web（workspace-calendar-v1 子切片 · 前端 lane）
Read: openspec/changes/workspace-calendar-v1/design.md
Tasks: workspace-calendar-v1/tasks.md §2
```
