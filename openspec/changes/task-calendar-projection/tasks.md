# Tasks：task-calendar-projection（母 change · 执行路线图）

> **用法**：本文件是任务×日历投影联动的 **总路线图**。实际编码按 **子 change** 分批执行；每次会话只做一个子 change 内的一组 task（通常 ≤10 条）。  
> **顺序**：**前端优先**（`-web` → `-api` → `-integrate`）。  
> **拍板**：分域投影；虚拟「我的任务」图层；前端并行拉 due-range；不做时间盒/AI 排期/写 `cal_event`。

---

## 0. 规划基线（本 change）

- [x] 0.1 `proposal.md` / `design.md` / spec deltas（`calendar`、`task`、`user-preference`）/ 本 `tasks.md`
- [x] 0.2 `openspec validate task-calendar-projection --strict`
- [x] 0.3 更新 `docs/dev/api-integration-board.md` 登记 `task-calendar-projection`（planned）与切片顺序
- [x] 0.4 更新 `docs/dev/workspace-ui-patterns.md` 日历节：侧栏含「我的任务」虚拟图层（实现前可先改文档真源）

---

## 1. task-calendar-projection-web（前端 lane · 第一步）

**目标**：日历侧栏 + 网格投影 UI；contract 冻结 due-range 与 preference 键。  
**范围**：主要 `web/` + `openspec/lanes/task-calendar-projection/contract.md`。

- [ ] 1.1 起草 `openspec/lanes/task-calendar-projection/contract.md`（`GET due-range` 参数/响应、`showTaskLayer`、投影字段）
- [ ] 1.2 `api/app/task.ts` + store：due-range（临时数据仅 store；禁止常驻 `mocks/`）
- [ ] 1.3 `/app/calendar` 侧栏：「我的任务」勾选图层；进入页时读 `settings.calendar.showTaskLayer` 默认
- [ ] 1.4 日/周/月网格：渲染任务投影（与日程可区分）；点击 → `/app/tasks?taskId=`；禁止打开事件编辑器
- [ ] 1.5 设置窗「日历」：增加「显示我的任务图层」开关（绑 preference 形状）
- [ ] 1.6 `cd web && pnpm build && pnpm typecheck`
- [ ] 1.7 浏览器：`/app/calendar` 开关图层见/隐任务；点击进任务页；设置窗改默认

**验证**：`pnpm build` + `pnpm typecheck` + 浏览器路径。  
**完成后**：看板 web → `ui_ready`；可开 `-api`。

---

## 2. task-calendar-projection-api（后端 lane · 第二步）

**目标**：按 contract 实现 due-range；preference 默认键；可选 `TaskItemApi`。  
**范围**：Java；无业务 UI 改动（CORS/Security 除外）。

- [ ] 2.1 `GET /app-api/task/item/due-range`（from/to、TODO、本人、租户、limit≤200）
- [ ] 2.2 preference 代码默认合并 `settings.calendar.showTaskLayer=true`
- [ ] 2.3 （可选）`TaskItemApi` due-range 同语义；日历页不依赖服务端聚合
- [ ] 2.4 `./mvnw -pl relayflow-server -am compile` + curl 自测 due-range

**验证**：compile + curl `code=0`。  
**完成后**：看板 api → 可联调。

---

## 3. task-calendar-projection-integrate（联调 · 第三步）

- [ ] 3.1 去掉 calendar/task store 内 due-range 临时数据；只走真实 API
- [ ] 3.2 联调：有/无截止日、DONE 不出现、图层关不挡日程、深链 `taskId`
- [ ] 3.3 `pnpm build && pnpm typecheck`；`./mvnw -pl relayflow-server -am compile`
- [ ] 3.4 看板 → archived；子 change archive；母 change 勾选完成项

**验证**：联调路径 + 构建命令。

---

## 非本 change（后续可选）

- 日历上拖任务改 `due_time`
- Sunsama 式时间盒占忙闲
- Motion/Reclaim AI 排期
- 日历内勾选完成任务
