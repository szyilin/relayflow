# Tasks：workspace-task-core-v1（母 change · 执行路线图）

> **用法**：本文件是任务核心能力的 **总路线图**。实际编码按 **子 change** 分批执行；每次会话只做一个子 change 内的一组 task（通常 ≤10 条）。  
> **顺序**：**前端优先**；先 **detail（P0）** 再 **collab（P1）**。  
> **拍板**：套件内嵌协作任务；P0 详情/起止/子任务；P1 关注/评论/动态/指派；清单与看板不在本母 change。

---

## 0. 规划基线（本 change）

- [x] 0.1 `proposal.md` / `design.md` / spec delta（`task`）/ 本 `tasks.md`
- [x] 0.2 `openspec validate workspace-task-core-v1 --strict`
- [x] 0.3 更新 `docs/dev/api-integration-board.md` 登记 `workspace-task-core`（planned）与切片顺序
- [x] 0.4 更新 `docs/dev/workspace-ui-patterns.md`：`/app/tasks` 三栏 + 详情面板（实现前可先改文档真源）

---

## 1. workspace-task-detail-web（P0 · 前端第一步）

**目标**：详情面板 UI；contract 冻结详情/子任务字段。  
**范围**：`web/` + `openspec/lanes/workspace-task-detail/contract.md`。

- [x] 1.1 起草 `openspec/lanes/workspace-task-detail/contract.md`
- [x] 1.2 `/app/tasks`：中栏根任务列表 + 右侧详情面板（`?taskId=`）
- [x] 1.3 详情：标题、起止时间、提醒、描述、完成；子任务列表与进度（store 临时数据可）
- [x] 1.4 `cd web && pnpm build && pnpm typecheck`
- [ ] 1.5 浏览器：打开详情、编辑字段、增删完成子任务

**完成后**：看板 detail → `ui_ready`。

---

## 2. workspace-task-detail-api（P0 · 后端第二步）

- [x] 2.1 Flyway：`task_item` 增 `start_time` / `description` / `remind_before_minutes` / `parent_id`；索引
- [x] 2.2 codegen diff 合入 DO/Mapper；详情 get/update、子任务 CRUD REST
- [x] 2.3 page 默认仅根任务；深度限制 1 层
- [x] 2.4 `./mvnw -pl relayflow-server -am compile` + curl

**完成后**：可联调 detail。

---

## 3. workspace-task-detail-integrate（P0 · 联调）

- [ ] 3.1 去掉 detail 临时数据；只走 API
- [ ] 3.2 联调深链、子任务进度、非法时间窗
- [ ] 3.3 `pnpm build && pnpm typecheck`；`mvn compile`
- [ ] 3.4 看板 detail → done/archived

---

## 4. workspace-task-collab-web（P1 · 前端第一步）

- [ ] 4.1 起草 `openspec/lanes/workspace-task-collab/contract.md`
- [ ] 4.2 启用导航「我关注的」「动态」；详情：关注人、评论、活动流、指派
- [ ] 4.3 `pnpm build && pnpm typecheck` + 浏览器路径

**完成后**：看板 collab → `ui_ready`。

---

## 5. workspace-task-collab-api（P1 · 后端第二步）

- [ ] 5.1 Flyway：`task_follower` / `task_comment` / `task_activity`
- [ ] 5.2 关注、评论、动态、指派 REST；成员校验 `system-api`；指派 `task-bot` best-effort
- [ ] 5.3 `./mvnw -pl relayflow-server -am compile` + curl

---

## 6. workspace-task-collab-integrate（P1 · 联调）

- [ ] 6.1 去临时数据；联调关注/评论/动态/指派与 Bot
- [ ] 6.2 构建验证；看板归档；母 change 勾选完成
- [ ] 6.3 （可选）`openspec archive` 或 sync specs

---

## 非本母 change（后续）

- 任务清单 Tasklist / 分组
- 看板视图
- 自定义字段、仪表盘、甘特
- 日历拖改 `due_time`、群聊建任务
