## Context

- 现状：`task_item` 已支持详情字段、子任务、关注/评论/动态/指派；`/app/tasks` 三栏 + 详情 slideover；左栏为个人入口（我负责的 / 我创建的 / 已完成 / 我关注的 / 动态）；「看板」Tab 为空态占位。
- 缺口：无任务清单容器；任务无法按项目组织与共享；看板无真数据。
- 约束：仍在 `relayflow-module-task`；跨域仅 `*-api`；前端优先切片；表前缀 `task_`；触达不强制本切片扩展 Bot。
- 前序：`workspace-task-core-v1` 明确将「清单 / 看板」列为下一母 change。

## Goals / Non-Goals

**Goals:**

- P0：任务清单 CRUD、成员、任务 `list_id` 归属；左栏清单导航；清单内列表；深链 `?listId=`
- P1：清单上下文看板三列（TODO / IN_PROGRESS / DONE）+ 拖拽改状态；列内排序可落库
- 个人池（`list_id IS NULL` 或「我负责的」按 assignee）行为保持可用
- 复用现有详情面板与协作能力，不重做

**Non-Goals:**

- 自定义看板列、自定义字段、仪表盘、甘特
- 全租户公开清单、外部访客
- 跨清单看板、复杂筛选
- 管理端治理、群聊/文档一键进清单
- 强制扩展 `task-bot` 模板（可选后续）

## Decisions

### D1：清单为可选容器（`list_id` 可空）

- **选择**：`task_item.list_id` 可空；未挂清单的任务继续出现在「我负责的」等个人视图。
- **理由**：兼容已有数据与心智；不强行「一切皆项目」。
- **备选**：强制所有任务进默认清单 — 否（迁移与 UX 成本高）。

### D2：表结构

```text
task_list (
  id, tenant_id, name, description?, owner_id,
  archived?,  -- 软归档可选；或仅 deleted
  creator, create_time, updater, update_time, deleted
)

task_list_member (
  id, tenant_id, list_id, user_id,
  role  -- OWNER | EDITOR | VIEWER
  …公共字段
)
-- UNIQUE (tenant_id, list_id, user_id) WHERE deleted = 0

task_item 增量：
  list_id BIGINT NULL
  -- status CHECK 扩展含 IN_PROGRESS
  board_rank INT NULL  -- 同 list + status 列内排序；可空则按 update_time/id
```

- **创建清单**：写 `task_list` + 插入创建者为 `OWNER` 成员行（A 类：成员行必须物化）。
- **codegen**：`./scripts/codegen.sh --tables …` → 临时目录 → diff 合入；禁止从零手写 DO。

### D3：权限模型

| 资源 | 规则 |
|------|------|
| 无 `list_id` 任务 | 维持现有：负责人/创建人可写；关注人只读+评论 |
| 清单可见 | 当前用户为 `task_list_member` 且未删 |
| 清单写（改名/归档/邀成员/移出成员） | `OWNER`（邀成员可由 OWNER；EDITOR 是否可邀：**V1 仅 OWNER**） |
| 清单内任务写 | `OWNER` / `EDITOR`，或该任务的 assignee/creator（与现有一致时取并集） |
| 清单内任务读 | 成员均可；VIEWER 只读核心字段，可评论（对齐 follower） |
| 「我负责的」 | 仍按 `assignee_id = me` 根任务，**含挂在清单内的** |

- 成员用户 MUST 经 `system-api` 校验为当前租户 ACTIVE 成员。
- **禁止**非成员靠猜 id 读写清单任务。

### D4：状态三态（看板列 = B1）

- **选择**：`status ∈ { TODO, IN_PROGRESS, DONE }`；看板三列一一对应。
- **勾选完成**：`DONE`；从 DONE 取消勾选 → `TODO`（不自动恢复 IN_PROGRESS，除非产品另定；**拍板：回 TODO**）。
- **拖拽**：改 `status` + 更新 `board_rank`（目标列内位置）。
- **列表筛选**：既有 `status` query 支持三态；「已完成」视图仍筛 `DONE`。
- **备选**：自定义 `task_board_column` — 后置。

### D5：UI 结构

```text
左栏：个人入口（现有）+ 「清单」分组（我的清单列表 + 新建）
中栏：
  - 个人入口：列表为主；看板 Tab 在个人入口下隐藏或保持占位文案「请打开清单使用看板」
  - 清单上下文：列表 | 看板；创建默认带 listId
右栏：现有 TaskDetailPanel（slideover）
深链：?listId=&taskId=&view=  （listId 优先切换到清单上下文）
```

- 工作台壳层、token：遵循 `workspace-ui-patterns` / `workspace-ui-tokens`。
- 看板拖拽：优先轻量实现（HTML5 DnD 或已有依赖）；不引入重量级看板库 unless contract 需要。

### D6：API 边界（草案，`-web` 起草 contract 时细化）

| 前缀 | 用途 |
|------|------|
| `/app-api/task/list/*` | 清单 CRUD、我的清单、归档 |
| `/app-api/task/list/member/*` | 成员列表/邀请/移除/改角色 |
| `/app-api/task/item/*` | 既有 + `listId` 过滤/创建归属；`PUT …/status` 或 `PUT …/board-move` |

- 清单内任务分页：`GET /item/page?listId=`（成员可见范围内全部根任务，不只 assignee=me）。
- 「我负责的」：`listId` 不传，行为不变。

### D7：默认数据（A/B/C）

- 清单：**非**成员入企 ensure；用户显式创建（非 A 类预插）。
- 成员行：创建清单时插入 OWNER（与清单同事务）— 对清单实体而言属 A。
- 偏好：看板/列表 Tab 记忆可后续用 user-preference（C）；本母 change 可不做，前端 session 即可。

### D8：实现切片

| 切片 | 重心 |
|------|------|
| list-web / api / integrate | 表 list+member、`list_id`、左栏、清单列表、成员、深链 |
| board-web / api / integrate | `IN_PROGRESS`、看板 UI、拖拽/`board_rank`、个人入口看板策略 |

### D9：无新端口 / 模块

- 仍 `relayflow-module-task`；无 Gateway；Flyway 增量（建议 `V0.1.0.25+`，以当时最大序号为准）。

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| 「我负责的」与清单内任务重复出现 | 产品接受：个人收件箱含清单任务；清单视图看全员任务；列表行可显示所属清单名（list-web 可选） |
| status 三态破坏旧客户端 | 仅工作台一客户端；CHECK 迁移；旧 TODO/DONE 行无需数据回填 |
| 权限并集复杂 | contract 写死矩阵；单测覆盖非成员 403 |
| 看板排序并发 | `board_rank` 用整数间隔或整列重排；V1 可接受最后写入胜 |
| 拖拽库膨胀 | 先原生 DnD；不达标再评估 |

## Migration Plan

1. 母版 validate；看板登记「建议下一切片」→ 规划中  
2. list-web → list-api（Flyway + codegen diff）→ integrate  
3. board-web → board-api → integrate  
4. 回滚：UI 隐藏清单/看板；`list_id` 空任务不受影响；可保留表  

## Open Questions

- EDITOR 是否允许邀请成员 — **本设计暂定否（仅 OWNER）**；若产品要改，在 list-api contract 改一处即可。
- 归档清单后任务是否仍出现在「我负责的」— **是**（任务不级联删）；清单中栏不再默认展示已归档清单。
- 子任务是否继承父 `list_id` — **是**（创建子任务时拷贝父 list_id；不可单独挂到另一清单，V1）。
