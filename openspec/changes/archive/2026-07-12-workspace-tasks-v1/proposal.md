# 提案：工作台任务 V1（workspace-tasks-v1 · 母 change · 执行路线图）

## Why

工作台左侧导航已有 **「任务」** 入口（`/app/tasks`），但页面仅为空壳占位（`tasks = []`、新建按钮 disabled），[`api-integration-board.md`](../../../docs/dev/api-integration-board.md) 标注 **仍为 Mock**。

飞书「任务」V1 最小可用：**我负责的任务列表、新建、完成勾选、截止日期**；看板/协作/子任务属后续。补齐后工作台四条主导航中 **消息 + 任务** 两条具备实质能力，产品体感显著提升。

## What Changes

本 change 为 **母 change（规划真源）**；实现按纵向切片拆子 change，默认 **前端优先**（`-web` → `-api` → `-integrate`）。`[平台]` 子 change 可先行。

1. **新 Maven 域** `relayflow-module-task`（`*-api` + `*-biz`），表前缀 `task_`
2. **数据模型**：`task_item` — 标题、负责人、创建人、截止时间、状态（`TODO`/`DONE`）、租户隔离
3. **App API**：`GET/POST/PUT /app-api/task/item/*` — 当前用户「我负责的」CRUD + 完成切换
4. **前端**：`/app/tasks` 列表视图接 store；启用「新建」；看板 tab 保持占位或只读分组（V1 可延后看板数据）
5. **看板**：`api-integration-board` 登记 `workspace-tasks` 切片

## Capabilities

### New Capabilities

- `task`：租户内个人任务项（V1 不含项目/子任务/评论）

### Modified Capabilities

- （无；工作台 UI 行为记入 `task` 新 spec，不修改 `web-auth`）

## Impact

| 层 | 模块/路径 | 变更 |
|----|-----------|------|
| Maven | 根 `pom.xml`、`relayflow-server/pom.xml` | 引入 `relayflow-module-task-biz` |
| DB | Flyway `V0.1.0.x` | `task_item` |
| 后端 | `relayflow-module-task-*` | 新建模块 |
| 前端 | `web/src/pages/app/tasks/`、`stores/tasks.ts`、`api/app/task.ts` | 去页面内 Mock |
| 规格 | `openspec/specs/task`（新建） | 归档时同步 |

## 非目标

- 任务分配给他人、关注人、动态流（侧栏「我关注的」「动态」保持 disabled/占位）
- 看板拖拽、自定义列表、标签、优先级、附件
- 与 IM / 审批 / 文档联动
- 管理端任务管理
- `relayflow-module-bpm` 工作流任务（V1.1）

## 前置

- 统一登录、工作台壳层、多租户 JWT 已就绪
- 无硬依赖 `org-member-invite-notify`（可并行）

## 子 change 切片（实现顺序）

```text
workspace-tasks-v1                ← 本 change（规划母版）
├── task-schema-v1                [平台] Flyway + Maven 模块脚手架 + codegen
├── workspace-tasks-web           UI + Mock + contract
├── workspace-tasks-api           CRUD REST
└── workspace-tasks-integrate     去 Mock 联调
```
