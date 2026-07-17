# 提案：工作台任务核心能力（workspace-task-core-v1 · 母 change）

## Why

工作台任务目前仅有「我负责的」标题级 CRUD + 截止提醒 + 日历投影，与飞书式协作任务差距过大：无详情面板、起止时间、描述、子任务、关注人、评论与动态。日历联动已打通时间入口，但**任务对象本体尚未真正产品化**。现在应用套件定位把任务做成可协作的工作对象，而不是继续堆个人待办字段。

## What Changes

本 change 为 **母 change（规划真源 + 产品形态拍板）**；实现按纵向切片拆子 change，默认 **前端优先**（`-web` → `-api` → `-integrate`）。

### 产品形态（拍板）

> **协作套件内的工作任务**：壳层与触达学飞书；任务详情清晰度对齐 Asana；交互克制学 Linear；时间与日历对齐 TickTick/飞书。  
> **不做** ClickUp 全能台、**不做** Jira/Linear 工程缺陷系统。

| 层 | 对标 | 本母 change 范围 |
|----|------|------------------|
| 套件契合 | 飞书 | 三栏（导航/列表/详情）、我负责的·我关注的·动态、Bot/深链 |
| 任务对象 | Asana / 飞书详情 | 标题、负责人、起止时间、提醒、描述、子任务、完成 |
| 协作 | 飞书 / Asana | 关注人、评论、变更动态 |
| 明确后置 | — | 清单容器、看板、自定义字段、仪表盘、甘特 |

### 能力增量（按切片交付）

1. **详情对象（P0）**：`/app/tasks` 右侧详情面板；`start_time` / `due_time` / 提醒偏移 / 描述；子任务与进度；完成主操作  
2. **协作与动态（P1）**：关注人、「我关注的」、评论、任务/个人动态流；指派他人（负责人可改）；`task-bot` 触达扩展（指派/关注类，best-effort）  
3. **文档**：更新 `workspace-ui-patterns`、API 对接看板；lane contract 由各 `-web` 起草  

**BREAKING（相对现前端心智）**：任务从「一行勾选」升级为「可打开详情的协作对象」；表结构扩展（增量 Flyway，兼容旧行：`start_time` 可空、描述可空）。

## Capabilities

### New Capabilities

（无——行为归入既有 `task`；触达仍走既有 `im` Bot 契约，不新开域）

### Modified Capabilities

- `task`：从个人待办 CRUD 扩展为协作任务对象（详情字段、子任务、关注、评论、动态、侧栏入口启用）
- `im`（可选增量）：若新增指派/关注触达模板，仅扩展 `task-bot` 用法说明；不改 Bot 平台模型

## Impact

| 层 | 模块/路径 | 变更 |
|----|-----------|------|
| DB | Flyway `task_*` | 扩展 `task_item`；新增子任务/关注/评论/动态等表（见 design） |
| 后端 | `relayflow-module-task-*` | REST + 可选 `TaskItemApi` 扩展；→ `system-api` 校验成员；→ `im-api` Bot |
| 前端 | `web/` `/app/tasks` | 三栏壳 + 详情面板 + 动态/关注 Tab |
| 日历 | 既有投影 | 继续读 `due_time`；有 `start_time` 时展示规则见 design（默认仍以截止投影为主） |
| 文档 | 看板、UI patterns、lane contract | 登记切片 |
| 回滚 | 关 UI 新区块；表可保留 | 旧客户端仍可用 page/create/toggle |

## 非目标（本母 change / 近端）

- 任务清单（Tasklist）容器与清单成员权限 — **下一母 change**
- 看板 / 仪表盘 / 甘特 / 自定义字段
- 日历上拖任务改期、时间盒占忙闲、AI 排期
- 文档内嵌任务、群聊一键建任务（可后续）
- 管理端任务管理

## 拍板结论（2026-07-17）

| 项 | 结论 |
|----|------|
| 产品赛道 | 套件内嵌协作任务（飞书同赛道），非专项 PM |
| P0 | 详情面板 + 起止/提醒/描述/子任务 |
| P1 | 关注 + 评论 + 动态 +「我关注的」+ 可指派 |
| 清单 / 看板 | **不在本母 change 实现切片内** |
| 实现顺序 | detail → collab；每段 `-web` → `-api` → `-integrate` |

## 子 change 切片（实现顺序）

```text
workspace-task-core-v1                 ← 本 change（规划母版）
├── workspace-task-detail-web          P0 UI + contract
├── workspace-task-detail-api          表扩展 + 详情/子任务 REST
├── workspace-task-detail-integrate
├── workspace-task-collab-web          P1 UI + contract
├── workspace-task-collab-api          关注/评论/动态/指派 + Bot
└── workspace-task-collab-integrate
```
