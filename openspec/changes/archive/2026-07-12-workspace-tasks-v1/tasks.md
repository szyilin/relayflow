# Tasks：workspace-tasks-v1（母 change · 执行路线图）

> **用法**：本文件是工作台任务的 **总路线图**。实际编码按 **子 change** 分批执行；每次会话只做一个子 change 内的一组 task（≤10 条）。  
> **顺序**：默认 **前端优先**（`-web` → `-api` → `-integrate`）；`[平台]` 子 change 可先行。

---

## 0. 规划基线（本 change）

- [x] 0.1 `proposal.md` / `design.md` / spec delta / 本 `tasks.md`
- [x] 0.2 `openspec validate workspace-tasks-v1 --strict`
- [x] 0.3 更新 `docs/dev/api-integration-board.md` 登记 `workspace-tasks`（planned）

---

## 1. [平台] task-schema-v1

**目标**：`task_item` 表 + Maven 模块 + server 加载。  
**范围**：Java only；无 `web/`。

- [x] 1.1 根 `pom.xml` + `relayflow-module-task`（api + biz）脚手架（复制 system 模块模式）
- [x] 1.2 Flyway `V0.1.0.{n}__task_item.sql`
- [x] 1.3 `./scripts/codegen.sh --module task --tables task_item` → diff 合并
- [x] 1.4 `relayflow-server/pom.xml` 引入 `relayflow-module-task-biz`；空 Controller 占位可启动
- [x] 1.5 `./mvnw -pl relayflow-server -am compile`

**验证**：compile + Flyway 迁移成功。

**完成后**：可开 `workspace-tasks-web`。

---

## 2. workspace-tasks-web（前端 lane · 第一步）

- [x] 2.1 起草 `openspec/lanes/workspace-tasks/contract.md`
- [x] 2.2 `api/app/task.ts`、`mocks/tasks.ts`、`stores/tasks.ts`（Mock 回退）
- [x] 2.3 重构 `/app/tasks`：列表接 store；启用「新建」`UModal`；checkbox 切换
- [x] 2.4 侧栏「我关注的」「动态」disabled + tooltip
- [x] 2.5 看板 tab 保持 `UEmpty` 占位
- [x] 2.6 `cd web && pnpm build`
- [x] 2.7 浏览器：`/app/tasks` → Mock 新建/完成

**验证**：`pnpm build` + 浏览器路径。

**完成后**：看板 web → `ui_ready`；可开 `-api`。

---

## 3. workspace-tasks-api（后端 lane）

**依赖**：`task-schema-v1` 完成、`workspace-tasks-web` contract 就绪

- [x] 3.1 `TaskItemService` + `controller/app/TaskItemController`
- [x] 3.2 `GET page`、`POST create`、`PUT update`、`PUT toggle-done`、`DELETE delete`
- [x] 3.3 错误码 `TASK_NOT_FOUND`、`TASK_FORBIDDEN`；仅 assignee=当前用户
- [x] 3.4 Security：`/app-api/task/**` JWT + 成员身份
- [x] 3.5 单元测试 + curl（见 contract）
- [x] 3.6 `./mvnw -pl relayflow-server -am compile`

**完成后**：看板 api → `ready`；开 `-integrate`。

---

## 4. workspace-tasks-integrate（联调）

- [x] 4.1 `stores/tasks.ts` 去 Mock
- [x] 4.2 端到端：新建 → 刷新仍在 → 勾选完成 → 删除
- [x] 4.3 多租户：切换企业后任务列表隔离
- [x] 4.4 `openspec validate workspace-tasks-v1 --strict`
- [x] 4.5 `./mvnw -pl relayflow-server -am compile` + `cd web && pnpm build`
- [x] 4.6 看板 `workspace-tasks` → **done**

---

## 5. 母 change 归档前

- [x] 5.1 全部子 change archive
- [x] 5.2 `openspec archive workspace-tasks-v1`（新建 `openspec/specs/task/spec.md`）
- [x] 5.3 `./mvnw verify`（如适用）+ `cd web && pnpm build`

---

## 执行顺序速查

```text
Session 1   §1 task-schema-v1
Session 2   §2 workspace-tasks-web
Session 3   §3 workspace-tasks-api
Session 4   §4 integrate + §5 归档
```

## 后续 change（不在本路线图）

| Change | 说明 |
|--------|------|
| `workspace-tasks-assign` | 指派他人、我关注的 |
| `workspace-tasks-board` | 看板视图 |
| `workspace-tasks-notify` | 截止提醒 → NotifyInbox |

---

## 会话开场白模板

```text
Using change: workspace-tasks-web（workspace-tasks-v1 子切片 · 前端 lane）
Read: openspec/lanes/workspace-tasks/contract.md
Tasks: workspace-tasks-v1/tasks.md §2
```
