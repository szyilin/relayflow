# 设计：工作台任务核心能力

## Context

- 现状：`task_item` 仅 title / status / due_time / assignee；`/app/tasks` 单栏列表；侧栏「我关注的」「动态」占位；日历投影只读 `due_time`。
- 约束：跨域仅 `*-api`；触达仅 `ImBotApi` + `task-bot`；前端优先切片；表前缀 `task_`。
- 产品：套件内嵌协作任务（飞书赛道），详情清晰（Asana），克制（Linear）。

## Goals / Non-Goals

**Goals:**

- P0：详情面板 + 起止时间、提醒、描述、子任务
- P1：关注、评论、动态、「我关注的」、可改负责人（指派）
- 保持与日历投影兼容（`due_time` 仍为投影主键）
- 数据模型可扩展到后续清单，但不在本母 change 建清单表

**Non-Goals:**

- Tasklist / 看板 / 自定义字段 / 仪表盘
- 日历拖改截止、时间盒、AI
- 群聊/文档内嵌创建（后续）

## Decisions

### D1：单表扩展 + 卫星表（非重写）

- **选择**：`task_item` 增加 `start_time`、`description`、`remind_before_minutes`（可空）；子任务 / 关注 / 评论 / 动态用独立 `task_*` 表。
- **理由**：兼容现有 CRUD 与 due 投影；避免大迁移。
- **备选**：父子任务全用邻接表一张 — 否（子任务进度与权限更清晰用 `parent_id` 同表或 `task_subtask`）。

### D2：子任务模型

- **选择**：子任务也是 `task_item` 行，`parent_id` 指向父任务；列表「我负责的」默认只展示根任务（`parent_id IS NULL`）；详情内嵌子任务列表。
- **理由**：对齐飞书「子任务本质是任务」；可独立指派/截止（P1 可用）；进度 = 已完成子任务数 / 子任务总数。
- **约束**：V1 子任务深度 **1 层**（子任务不可再挂子任务），降低 UI/查询复杂度。

### D3：时间字段

- **选择**：`start_time`、`due_time` 均为可空 `TIMESTAMPTZ`；提醒用 `remind_before_minutes`（相对 **due_time**，0=不提醒/沿用系统窗可另议）。
- **与 Bot**：现有 `relayflow.task.due-remind-window` 补偿逻辑保留；详情里显式提醒偏移在 collab/detail-api 中与 Bot 对齐（design 拍板：有 `remind_before_minutes` 时按 due−offset 触发，否则沿用 window）。
- **日历**：投影仍只用 `due_time`；有 start 无 due 的不进图层（本母 change 不改投影契约，除非另开）。

### D4：详情 UI

- **选择**：`/app/tasks` 保持左导航 + 中列表；**右侧详情面板**（飞书式），深链 `?taskId=` 打开详情。
- **理由**：与现有深链兼容；不必先做独立全页。

### D5：权限（P0/P1）

- **P0**：创建人/负责人可编辑详情与子任务；他人不可见个人任务（仍「我负责的」范围）。
- **P1**：关注人只读详情+可评论；负责人可改 `assignee_id`（指派，校验 `system-api` 租户成员）；被指派者进入「我负责的」。
- **动态可见**：本人相关（负责/关注/创建）的事件；不做租户全局动态墙。

### D6：动态与评论

- **选择**：`task_activity` 追加写（类型：created / field_changed / subtask_* / follower_* / commented / assigned…）；评论存 `task_comment`，同时写一条 activity。
- **理由**：动态页可读 activity；评论可分页与富文本后续扩展（V1 纯文本）。
- **不**把评论做成 IM 消息；触达仍 Bot DM。

### D7：实现切片边界

| 切片 | 表 / API 重心 |
|------|----------------|
| detail | `task_item` 扩展、`parent_id`、详情/子任务 REST |
| collab | `task_follower`、`task_comment`、`task_activity`、指派、关注列表、动态 API |

### D8：无新端口 / 模块

- 仍在 `relayflow-module-task`；无 Gateway；Flyway 仅增量。

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| 子任务同表导致「我负责的」噪音 | 根任务过滤；子任务仅详情展示 |
| 动态表膨胀 | 仅关键字段变更写 activity；保留策略后续 |
| 指派与日历投影 | 被指派后 due 投影跟新负责人 |
| 提醒双重逻辑 | detail-api 明确 remind 与 window 优先级并写单测 |

## Migration Plan

1. 规划母版 validate + 看板登记  
2. detail-web → detail-api（Flyway + codegen diff）→ integrate  
3. collab-web → collab-api → integrate  
4. 回滚：UI 关详情/协作；旧字段仍可用  

## Open Questions

- 提醒：显式 `remind_before_minutes` 与全局 `due-remind-window` 并存时的精确优先级 — **实现 detail-api 前在 contract 写死**（建议：有显式值则按 due−minutes；否则 window）。
- 多人负责人 — **本母 change 不做**（单 assignee）。
