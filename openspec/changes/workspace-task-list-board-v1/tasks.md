# Tasks：workspace-task-list-board-v1（母 change · 执行路线图）

> **用法**：本文件是任务清单 + 看板的 **总路线图**。实际编码按 **子 change** 分批执行；每次会话只做一个子 change 内的一组 task（通常 ≤10 条）。  
> **顺序**：**前端优先**；先 **list（P0）** 再 **board（P1）**。  
> **拍板**：清单可选（`list_id` 可空）；成员 OWNER+显式邀请；看板 = 状态三列（TODO / IN_PROGRESS / DONE）；自定义列与自定义字段后置。

---

## 0. 规划基线（本 change）

- [x] 0.1 `proposal.md` / `design.md` / spec delta（`task`）/ 本 `tasks.md`
- [x] 0.2 `openspec validate workspace-task-list-board-v1 --strict`
- [x] 0.3 更新 `docs/dev/api-integration-board.md`：建议下一切片 → 登记 `workspace-task-list` / `workspace-task-board`（planned）与切片顺序
- [x] 0.4 更新 `docs/dev/workspace-ui-patterns.md`：`/app/tasks` 左栏清单 + 清单内列表/看板（实现前可先改文档真源）
- [x] 0.5 （可选）`AGENTS.md`「下一优先」改为本母 change 已立项

---

## 1. workspace-task-list-web（P0 · 前端第一步）

**目标**：左栏清单、清单内列表、成员 UI；contract 冻结清单/成员/`listId`。  
**范围**：`web/` + `openspec/lanes/workspace-task-list/contract.md`。

- [x] 1.1 起草 `openspec/lanes/workspace-task-list/contract.md`（list CRUD、member、item `listId`、深链）
- [x] 1.2 `/app/tasks` 左栏：「清单」分组 + 新建；选中清单切换中栏上下文（`?listId=`）
- [x] 1.3 清单中栏：根任务列表；创建默认带 `listId`；成员管理（邀请/角色展示）；store 临时数据可
- [x] 1.4 个人入口行为保持；详情 slideover 复用
- [x] 1.5 `cd web && pnpm build && pnpm typecheck`；浏览器路径写明

**完成后**：看板 list → `ui_ready`。

**浏览器验证**：`/app/login` → `/app/tasks` → 左栏「清单」点种子/新建 → 中栏建任务 →「成员」邀请 → `?listId=` 深链；个人入口仍可用。

---

## 2. workspace-task-list-api（P0 · 后端第二步）

- [x] 2.1 Flyway：`task_list`、`task_list_member`；`task_item.list_id` 可空 + 索引（序号以当时最大 Flyway 为准）
- [x] 2.2 codegen diff 合入 DO/Mapper；清单/成员 REST；创建清单同事务写 OWNER
- [x] 2.3 item create/page 支持 `listId`；权限矩阵按 design；成员校验 `system-api`；子任务继承 `list_id`
- [x] 2.4 `./mvnw -pl relayflow-server -am compile` + curl

**完成后**：可联调 list。

**验证**：`./mvnw -pl relayflow-server -am compile` 通过；Flyway `V0.1.0.25` 已 migrate；curl 需起 `relayflow-server` 后按 `openspec/lanes/workspace-task-list/contract.md` 验收。

---

## 3. workspace-task-list-integrate（P0 · 联调）

- [x] 3.1 去掉 list 临时数据；只走 API
- [x] 3.2 联调：建清单、邀成员、清单内建任务、深链、非成员 403、归档不删任务
- [x] 3.3 `pnpm build && pnpm typecheck`；`mvn compile`
- [x] 3.4 看板 list → done/archived

**联调记录**：`19988888888` 登录 → create list / page?listId / members / archive；归档后 mine 为空且任务 get 仍可读。成员邀请依赖通讯录搜索（UI 已强制先选人）。

---

## 4. workspace-task-board-web（P1 · 前端第一步）

**目标**：清单内看板三列 + 拖拽；contract 冻结 status/board-move。  
**范围**：`web/` + `openspec/lanes/workspace-task-board/contract.md`。

- [x] 4.1 起草 `openspec/lanes/workspace-task-board/contract.md`（三态、拖拽、`board_rank`）
- [x] 4.2 清单上下文「列表 | 看板」：三列卡片；拖拽跨列/列内；点开详情
- [x] 4.3 个人入口：隐藏看板 Tab 或展示「请打开清单使用看板」（去掉空占位误导）
- [x] 4.4 `pnpm build && pnpm typecheck` + 浏览器路径

**完成后**：看板 board → `ui_ready`。

**浏览器验证**：`/app/tasks?listId=…` →「看板」三列 → 拖到「进行中」；点卡片开详情；个人入口无看板 Tab。拖拽暂存于 store（`USE_LOCAL_BOARD_MOVE`），待 board-api。

---

## 5. workspace-task-board-api（P1 · 后端第二步）

- [ ] 5.1 Flyway：`status` CHECK 含 `IN_PROGRESS`；`board_rank`（可与 list-api 合并迁移若尚未合入）
- [ ] 5.2 status / board-move REST；due-range 与 due Bot 将 `IN_PROGRESS` 视为未完成
- [ ] 5.3 toggle-done：done→`DONE`，取消→`TODO`；VIEWER 不可改状态
- [ ] 5.4 `./mvnw -pl relayflow-server -am compile` + curl

---

## 6. workspace-task-board-integrate（P1 · 联调）

- [ ] 6.1 去临时数据；联调拖拽落库、刷新保序、权限
- [ ] 6.2 构建验证；看板归档；母 change 勾选完成
- [ ] 6.3 （可选）`openspec archive` 或 sync specs 至 `openspec/specs/task`

---

## 非本母 change（后续）

- 自定义看板列、自定义字段、仪表盘、甘特
- 清单搜索进全局 ⌘K、Bot「加清单」触达
- 群聊/文档一键建任务进清单
- `/app/docs` 云文档产品化
