## Why

`workspace-task-list-board-v1` 已交付清单容器与「固定三列状态看板」，但与飞书任务心智仍有差距。飞书左栏前半是**带默认查询的个人快捷视图**（非清单容器）；看板列来自**工具栏分组**；「我负责的」另有**个人逻辑自定义分组**；任务可多清单；负责人可多选；「我分配的」依赖**分配人**。若不把目标态拍板，会继续在「状态=列 / 特殊清单」错误模型上堆功能。

## What Changes

本 change 为 **母 change（规划真源 + 产品形态拍板）**；实现按纵向切片拆子 change，默认 **前端优先**（`-web` → `-api` → `-integrate`）。**确认前不实现业务代码。**

### 产品形态（拍板 · 2026-07-18，修订同日晚）

| 项 | 结论 |
|----|------|
| 左栏三类 | **(A) 个人入口**「我负责的 / 我关注的 / 动态」；**(B) 快速访问**「全部任务 / 我创建的 / 我分配的 / 已完成」= 预设查询快捷方式；**(C) 任务清单** = 协作容器（成员、自定义字段、清单内组）。A/B **不存任务**。 |
| 快速访问 | V1 **全做**。各入口带**默认筛选（及常用排序/完成态）**；工具栏可再改；配置 **按账号私有**；**不是**清单，**不**挂清单级自定义字段定义。 |
| 「我负责的」默认条件 | 负责人（多选）**包含**当前用户。 |
| 「我负责的」自定义分组（计划 B） | **要做**：挂在「当前用户 × 我负责的」上的**个人逻辑分区**（默认组 + 自建组），仅本人可见；与工具栏「按字段分组」是两套能力。 |
| 工具栏「分组」 | **展示视图**：按系统/自定义字段切列或区块；空值 →「无分组」。列表与看板共用。 |
| 「我创建的」 | 创建人包含我。 |
| 「我分配的」 | 负责人**不包含**我 **且** 分配人**包含**我 → **分配人字段立项**。 |
| 「已完成」 | 完成态=已完成（常配按完成时间排序）。 |
| 「全部任务」 | 当前用户有权看见的任务集合（再叠未完成等默认条件，contract 细化）。 |
| 多清单 | **任务可属于多个清单**（**BREAKING**）。详情可按清单分别选清单内组。 |
| 自定义字段 | **仅清单侧**定义与共享；个人快捷视图不承载字段定义。 |
| 现有三列看板 | 过渡保留 → 目标改为字段分组 / 自定义组呈现。 |
| 实现范围 | **全都规划**：快速访问全家桶、视图配置、字段分组、我负责的个人组、分配人、多负责人、多清单、清单内组；自定义字段引擎可选后置。 |

### 能力增量（规划层）

1. **快捷视图（Quick views）**：统一「上下文 + 默认 filter 种子 + 私有 ViewConfig」模型覆盖 MINE / FOLLOWING / ALL / CREATED / ASSIGNED_BY_ME / COMPLETED。
2. **视图配置**：`displayMode` / `groupBy` / `sort` / `filters` / `visibleFields`；个人上下文私有；清单共享默认（OWNER/EDITOR 可保存）。
3. **按字段分组（展示 A）** + **「无分组」**。
4. **「我负责的」个人自定义分组（逻辑 B）**：私有组定义 + 任务在个人收件箱的组归属。
5. **分配人**：指派操作写入；支撑「我分配的」。
6. **多负责人**、**多清单**、**清单内组**（共享语境）。
7. **文档与 lane**：对接看板、workspace-ui-patterns、子 change contract。

**BREAKING（相对已交付 list-board）**

- 看板列语义从「全局 status 三列」改为「当前视图 groupBy 或自定义组」。
- 任务清单归属从单 `list_id` 升级为多清单成员关系。
- 负责人从单 `assignee_id` 升级为多选；新增分配人语义。

### 相对飞书的确认（规划依据）

- 快速访问 /「我负责的」= **预设查询 + 个人视图偏好**，不是特殊清单。
- 个人入口的分组/排序/筛选/字段列、以及「我负责的」自定义组 = **仅当前账号**。
- 清单 = 容器：成员可见共享字段与清单内组；可保存清单级视图默认。
- 侧栏「清单文件夹」（整理多个清单）与清单内任务组不同 → **本母 change 清单文件夹可后置**，不阻塞任务视图模型。

## Capabilities

### New Capabilities

（无——行为仍归 `task` 域）

### Modified Capabilities

- `task`：快捷视图与默认筛选；私有/清单视图配置；按字段分组；「我负责的」个人自定义分组；分配人；多负责人；多清单；清单内组；替换固定三列看板心智

## Impact

| 层 | 模块/路径 | 变更 |
|----|-----------|------|
| DB | Flyway `task_*` | 视图配置；个人组；清单组；任务-清单成员；任务-负责人；分配人；迁移旧列 |
| 后端 | `relayflow-module-task-*` | 快捷视图查询；视图 CRUD；分组/个人组 API；指派写分配人；Bot/日历扩展 |
| 前端 | `web/` `/app/tasks` | 左栏快速访问；工具栏；字段分组看板；我负责的自定义组 |
| 文档 | 看板、UI patterns、lane | 登记子切片 |
| 依赖 | `workspace-task-list-board-v1` | 清单底座保留；看板列模型演进 |

## 非目标（近端可不实现，design 可留钩子）

- 自定义字段完整引擎（公式/关联等）——仅可选规划「单选作分组源」
- 甘特、仪表盘、跨清单超级看板
- 侧栏「清单文件夹」整理（可后续）
- 清单字段级权限、外部访客、管理端任务治理、独立 bitable

## 子 change 切片（建议实现顺序）

```text
workspace-task-view-model-v1                 ← 本 change（规划母版）
├── workspace-task-quick-views-web           P0 左栏快速访问 + 默认筛选种子（Mock）
├── workspace-task-quick-views-api
├── workspace-task-quick-views-integrate
├── workspace-task-view-config-web           P1 工具栏 ViewConfig 持久化壳
├── workspace-task-view-config-api
├── workspace-task-view-config-integrate
├── workspace-task-group-by-field-web        P2 按系统字段分组（列表+看板）
├── workspace-task-group-by-field-api
├── workspace-task-group-by-field-integrate
├── workspace-task-multi-assignee-web        P3 多负责人 +「我负责的」语义
├── workspace-task-multi-assignee-api
├── workspace-task-multi-assignee-integrate
├── workspace-task-assigner-web              P4 分配人 +「我分配的」真条件
├── workspace-task-assigner-api
├── workspace-task-assigner-integrate
├── workspace-task-mine-groups-web           P5 「我负责的」个人自定义分组（计划 B）
├── workspace-task-mine-groups-api
├── workspace-task-mine-groups-integrate
├── workspace-task-multi-list-web            P6 多清单归属
├── workspace-task-multi-list-api
├── workspace-task-multi-list-integrate
├── workspace-task-list-groups-web           P7 清单内默认组/自定义组
├── workspace-task-list-groups-api
├── workspace-task-list-groups-integrate
└── （可选）workspace-task-custom-field-*     P8 清单自定义单选作分组源
```

> 顺序：先 **快捷视图默认查询** → **视图配置壳** → **字段分组换看板心智** → **多负责人** → **分配人** → **我负责的个人组** → **多清单** → **清单内组** → 可选自定义字段。
