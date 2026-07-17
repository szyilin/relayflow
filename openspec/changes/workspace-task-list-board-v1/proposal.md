## Why

工作台任务已具备详情、子任务、关注/评论/指派与日历投影，但任务仍主要挂在「我负责的」个人收件箱；UI 上「看板」Tab 仅为占位。飞书式协作闭环依赖 **清单（容器）+ 看板（状态列视图）**——这是最短路径补齐「按项目组织任务」能力，且能复用现有 `task_*` 与详情面板，无需重做任务对象。

## What Changes

本 change 为 **母 change（规划真源 + 产品形态拍板）**；实现按纵向切片拆子 change，默认 **前端优先**（`-web` → `-api` → `-integrate`）。

### 产品形态（拍板）

> **清单先于看板**：无清单的「个人 TODO/DONE 两列看板」价值过薄，不单独交付。  
> **看板列 = 扩展状态（B1）**：`TODO` | `IN_PROGRESS` | `DONE`；拖拽改 `status`。自定义看板列（B2）后置。  
> **清单可选**：`list_id` 可空；未挂清单的任务继续走个人池，兼容现状。  
> **清单成员**：所有者 + 显式邀请（editor / viewer）；不做全企业默认可见。

| 层 | 本母 change 范围 |
|----|------------------|
| 清单 | 创建/改名/归档；左栏「我的清单」；任务归属 `list_id`；成员邀请 |
| 看板 | 清单上下文下「列表 \| 看板」；三列；拖拽改状态；卡片点开现有详情 |
| 明确后置 | 自定义列、自定义字段、仪表盘、甘特、跨清单看板、群聊/文档一键进清单 |

### 能力增量（按切片交付）

1. **清单（P0）**：左栏清单导航；清单内列表；建任务默认进当前清单；成员 CRUD；深链 `?listId=`
2. **看板（P1）**：清单内看板三列 + 拖拽落库；个人入口可不展示看板或仅占位隐藏
3. **文档**：更新 `workspace-ui-patterns`、API 对接看板；lane contract 由各 `-web` 起草

**BREAKING（相对前端心智）**：`status` 从二元扩展为三态；勾选完成仍映射 `DONE`，拖入「进行中」为 `IN_PROGRESS`。旧行仅有 `TODO`/`DONE`，迁移兼容。

## Capabilities

### New Capabilities

（无——行为归入既有 `task` 域，不新开 Maven 域）

### Modified Capabilities

- `task`：新增清单容器与成员；任务可选归属清单；`status` 增加 `IN_PROGRESS`；清单上下文看板视图与拖拽改状态

## Impact

| 层 | 模块/路径 | 变更 |
|----|-----------|------|
| DB | Flyway `task_*` | 新增 `task_list`、`task_list_member`；`task_item.list_id` 可空；status CHECK 扩展；可选 `list_rank`/`board_rank` |
| 后端 | `relayflow-module-task-*` | 清单/成员 REST；列表按 `listId` 过滤；拖拽/改状态；→ `system-api` 校验成员 |
| 前端 | `web/` `/app/tasks` | 左栏清单；清单中栏列表/看板；复用 `TaskDetailPanel` |
| Bot / 日历 | 既有 | 本母 change **不强制**扩展触达或投影契约（仍按 assignee + due） |
| 文档 | 看板、UI patterns、lane contract | 登记切片 |
| 回滚 | 关清单/看板 UI；表可保留；无 list 任务行为不变 | |

## 非目标（本母 change / 近端）

- 自定义看板列、自定义字段、仪表盘、甘特
- 清单字段级权限、外部访客、全租户公开清单
- 跨清单统一看板、复杂筛选器
- 管理端任务/清单治理
- 群聊/文档一键建任务进清单（可后续）
- 重做详情/协作（关注/评论/动态已交付）

## 拍板结论（2026-07-17）

| 项 | 结论 |
|----|------|
| 顺序 | 清单 → 看板 |
| 看板列 | B1 状态三列（TODO / IN_PROGRESS / DONE） |
| `list_id` | 可空；个人池保留 |
| 成员 | 所有者 + 显式邀请（editor \| viewer） |
| 完成语义 | 看板拖到完成列 = `DONE`，与勾选一致 |
| 实现顺序 | list → board；每段 `-web` → `-api` → `-integrate` |

## 子 change 切片（实现顺序）

```text
workspace-task-list-board-v1           ← 本 change（规划母版）
├── workspace-task-list-web            P0 UI + contract
├── workspace-task-list-api            表 + 清单/成员/归属 REST
├── workspace-task-list-integrate
├── workspace-task-board-web           P1 看板 UI + 拖拽 + contract
├── workspace-task-board-api           status 三态 + 排序/拖拽落库（可薄）
└── workspace-task-board-integrate
```
