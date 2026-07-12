# 设计：工作台任务 V1（workspace-tasks-v1）

## Context

- UI 真源：[`workspace-ui-patterns.md`](../../../docs/dev/workspace-ui-patterns.md)
- 现有占位：[`web/src/pages/app/tasks/index.vue`](../../../web/src/pages/app/tasks/index.vue)
- 架构：[`architecture.md`](../../../docs/dev/architecture.md) — 新域 `*-api` + `*-biz`，`server` 加载 `task-biz`
- 表前缀：在 `sys_/infra_/im_/bpm_` 之外新增 **`task_`**（本 change 立项时写入 `openspec/config.yaml` context 备注）

## Goals / Non-Goals

**Goals:**

- 飞书式 **「我负责的」** 任务列表：标题、截止日期、完成勾选
- 当前登录用户可 **新建 / 编辑标题与截止日 / 切换完成状态 / 删除自己的任务**
- 租户隔离：`task_item.tenant_id` 来自 JWT

**Non-Goals:**

- 指派给他人、我关注的、动态、看板拖拽
- 子任务、评论、附件、提醒通知
- 管理端、跨模块 API

## D1：Maven 模块

```text
relayflow-module-task/
├── relayflow-module-task-api/     # TaskItemApi（可选，V1 仅 REST 可无跨域调用）
└── relayflow-module-task-biz/
```

`relayflow-server/pom.xml` 增加：

```xml
<dependency>
  <groupId>com.relayflow</groupId>
  <artifactId>relayflow-module-task-biz</artifactId>
</dependency>
```

父 `pom.xml` `<modules>` 增加 `relayflow-module-task`。

## D2：数据模型 `task_item`

```sql
CREATE TABLE task_item (
  id              BIGINT PRIMARY KEY,
  tenant_id       BIGINT NOT NULL,
  title           VARCHAR(200) NOT NULL,
  assignee_id     BIGINT NOT NULL,       -- V1 恒为创建人/当前用户
  creator_id      BIGINT NOT NULL,
  due_time        TIMESTAMP,             -- nullable
  status          VARCHAR(16) NOT NULL,  -- TODO | DONE
  create_time     TIMESTAMP NOT NULL,
  update_time     TIMESTAMP NOT NULL,
  deleted         SMALLINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_task_item_assignee ON task_item (tenant_id, assignee_id, status) WHERE deleted = 0;
```

| 枚举 | 说明 |
|------|------|
| `TODO` | 未完成 |
| `DONE` | 已完成 |

V1 **不建** `task_list` 表；侧栏「我负责的」写死 filter `assignee_id = 当前用户`。

## D3：App API

前缀：`/app-api/task/item`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/page?pageNo=&pageSize=&status=` | 分页；默认 assignee=当前用户 |
| POST | `/create` | body `{ title, dueTime? }`；assignee/creator=当前用户 |
| PUT | `/update` | body `{ id, title?, dueTime? }` |
| PUT | `/toggle-done` | body `{ id, done: boolean }` |
| DELETE | `/delete?id=` | 软删 |

**鉴权**：JWT + 有效 `sys_tenant_user`；**不**检查 `sys_permission`。

**错误码**：`TASK_NOT_FOUND`、`TASK_FORBIDDEN`（操作非本人任务）

## D4：前端结构

```text
web/src/
├── api/app/task.ts
├── stores/tasks.ts          # Mock 回退 → integrate 去 Mock
├── mocks/tasks.ts           # 仅 store 引用
└── pages/app/tasks/index.vue  # 接 store；启用新建 UModal
```

### UI 行为

| 区域 | V1 |
|------|-----|
| Panel 侧栏 | 仅「我负责的」可点击；「我关注的」「动态」disabled + tooltip「即将推出」 |
| Main 列表 | `UCheckbox` 切换完成；点击标题 inline 编辑或 UModal |
| 新建 | `UButton` 打开 UModal：`title`（必填）、`dueTime`（UInput date 或 datetime-local） |
| 看板 tab | 保持 `UEmpty` 占位 |

### Store 方法（contract 草案）

```ts
fetchMyTasks(params?: { status?: 'TODO' | 'DONE' })
createTask(payload: { title: string; dueTime?: string })
updateTask(payload: { id: string; title?: string; dueTime?: string | null })
toggleTaskDone(id: string, done: boolean)
deleteTask(id: string)
```

## D5：Codegen

Flyway 落地后：

```bash
./scripts/codegen.sh --module task --tables task_item
```

DO 合并至 `target/generated-sources/`，禁止手写 DO。

## D6：与通知中心关系

任务截止提醒、@指派 → **不在 V1**；将来 `task-biz` 可调 `NotifyInboxApi`（`org-member-invite-notify` 落地后）。

## 验证

```bash
openspec validate workspace-tasks-v1 --strict
./mvnw -pl relayflow-server -am compile
cd web && pnpm build
# 浏览器：/app/tasks → 新建 → 勾选完成 → 刷新仍在
```

## 看板登记

| 切片 | 页面 | 端点 |
|------|------|------|
| workspace-tasks | `/app/tasks` | `/app-api/task/item/*` |

## 子 change 边界

| 子 change | 范围 |
|-----------|------|
| `task-schema-v1` | Flyway、pom 模块、空 biz 启动、codegen |
| `workspace-tasks-web` | UI + Mock + `openspec/lanes/workspace-tasks/contract.md` |
| `workspace-tasks-api` | Controller + Service + 单测 |
| `workspace-tasks-integrate` | store 接 API、看板 done |
