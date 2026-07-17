# 提案：任务 × 日历投影联动（task-calendar-projection）

## Why

工作台已有独立的任务（`/app/tasks`）与日历（`/app/calendar`），但截止日任务不会出现在日历上，用户仍需在两个入口来回切换。日历 V1 曾明确排除「我的任务」图层；现在任务与日历均已可用，应补上飞书/TickTick 式的**最小投影联动**，形成「沟通 + 待办 + 时间安排」可视闭环——同时保持 `task_` / `cal_` 分域，避免把任务做成日程或滑向全能 PM。

## What Changes

本 change 为 **母 change（规划真源）**；实现按纵向切片拆子 change，默认 **前端优先**（`-web` → `-api` → `-integrate`）。

产品定位（拍板）：

- **壳层**：对齐飞书（套件内嵌、Bot 触达、深链）
- **联动**：对齐 TickTick / 飞书「我的任务」图层——截止日任务以**虚拟图层**投影到日历，**不**写入 `cal_event`
- **深度**：不对齐 ClickUp/Jira；清单/指派/AI 排期/时间盒（Sunsama）不在本 change

具体变更：

1. **日历侧栏**：增加可勾选的「我的任务」虚拟图层（非真实 `cal_calendar`）
2. **网格投影**：日/周/月展示当前用户 `TODO` 且带 `due_time` 的任务标记（与日程视觉可区分）
3. **交互**：点击任务标记 → 深链 `/app/tasks?taskId=`（或页内轻预览后跳转）；**禁止**把任务拖成日程、禁止从日历创建「假日程」写 `cal_event`
4. **跨域契约**：`task-api` 提供按截止时间窗查询「我负责的」任务；`calendar-biz` / 前端 **不得**直查 `task_` 表
5. **偏好（可选最小）**：`settings.calendar.showTaskLayer` 默认 `true`；设置窗日历分类可开关
6. **文档**：更新 `workspace-ui-patterns`、API 对接看板；登记 lane contract

## Capabilities

### New Capabilities

（无——行为归入既有 `calendar` / `task` / `user-preference`）

### Modified Capabilities

- `calendar`：撤销「V1 MUST NOT 我的任务图层」；增加虚拟任务图层与投影展示/交互要求
- `task`：增加按 `due_time` 时间窗列出「我负责的」TODO 的 app-api（及供跨域读取的 `*-api` 契约，若日历服务端聚合需要）
- `user-preference`：`settings.calendar` 增加 `showTaskLayer`（默认 true）

## Impact

| 层 | 模块/路径 | 变更 |
|----|-----------|------|
| 后端 | `relayflow-module-task-*` | 截止时间窗查询 API；必要时 `TaskApi` 供 calendar 只读投影 |
| 后端 | `relayflow-module-calendar-*` | **可选**聚合 endpoint；或前端并行拉 task + event（见 design） |
| 后端 | `user-preference` | 默认键 `showTaskLayer` |
| 前端 | `web/` `/app/calendar`、store/api、设置窗 | 侧栏图层 + 网格渲染 + 深链 |
| 文档 | `workspace-ui-patterns`、`api-integration-board` | 登记联动与契约 |
| DB | 无新表；**不**把任务写入 `cal_event` | 无 Flyway（除非后续另开） |
| 回滚 | 关图层 UI + 停用新 API | 无数据迁移；删除前端图层即可回退体验 |

## 非目标（本 change）

- 任务与日程合并实体 / 任务自动变 `cal_event`
- AI 自动排期（Motion / Reclaim）
- 把任务拖成时间块占用忙闲（Sunsama 时间盒）——记为后续可选
- 任务清单、多人指派、子任务、看板、自定义字段
- 会议室、CalDAV、管理端任务/日历管理

## 拍板结论（2026-07-17）

| 项 | 结论 |
|----|------|
| 实体模型 | **分域**：`task_item` 与 `cal_event` 分离 |
| 联动形态 | **虚拟图层投影**截止日 TODO |
| 写路径 | 任务仍只经 task API；日历图层只读 |
| 时间盒 / AI 排期 | **不做**（本 change） |
| 实现顺序 | `-web` → `-api` → `-integrate` |

## 子 change 切片（实现顺序）

```text
task-calendar-projection              ← 本 change（规划母版）
├── task-calendar-projection-web      UI：侧栏图层 + 网格标记 + Mock/store 临时数据 + contract
├── task-calendar-projection-api      task 时间窗查询 + preference 键；必要时 TaskApi
└── task-calendar-projection-integrate 去临时数据联调
```
