## 1. 规划门禁（本母 change）

- [x] 1.1 审阅并确认 `proposal.md` 拍板表（快速访问全做、计划 B 个人组、分配人立项、多清单、字段分组）
- [x] 1.2 审阅并确认 `design.md` 决策 D0–D11（三套分组语义）与剩余 Open Questions
- [x] 1.3 `openspec validate workspace-task-view-model-v1 --strict` 通过
- [x] 1.4 更新 `docs/dev/api-integration-board.md` 子切片顺序与能力摘要（planned）
- [ ] 1.5 （可选）收尾/archive `workspace-task-list-board-v1` §6.3

## 2. P0 — 快速访问预设视图（子 change：quick-views）

- [x] 2.1 开 `workspace-task-quick-views-web`：左栏全部/我创建的/我分配的/已完成 + 默认筛选种子 Mock；contract
- [x] 2.2 开 `workspace-task-quick-views-api`：按 contextType 查询；写死 ALL 可见范围；CREATED/COMPLETED 真条件（ASSIGNED_BY_ME 可先占位至 P4）
- [x] 2.3 开 `workspace-task-quick-views-integrate`：去 Mock；浏览器点开各入口验证默认条件

## 3. P1 — 视图配置持久化（子 change：view-config）

- [x] 3.1 开 `workspace-task-view-config-web`：工具栏筛选/排序/分组/字段/列表|看板；按上下文读写 Mock
- [x] 3.2 开 `workspace-task-view-config-api`：私有 context 与 LIST 共享默认 REST；权限按 D1/D3
- [x] 3.3 开 `workspace-task-view-config-integrate`：验证两用户「我负责的」配置互不影响

## 4. P2 — 按系统字段分组（子 change：group-by-field）

- [x] 4.1 开 `workspace-task-group-by-field-web`：列表分区 + 看板列由字段 `groupBy` 驱动；「无分组」；拖拽 Mock
- [x] 4.2 开 `workspace-task-group-by-field-api`：分组查询与拖拽落库；废弃说明旧 `board-move`
- [x] 4.3 开 `workspace-task-group-by-field-integrate`：替换写死三列唯一看板

## 5. P3 — 多负责人（子 change：multi-assignee）

- [x] 5.1 开 `workspace-task-multi-assignee-web`：详情负责人多选；MINE=包含我
- [x] 5.2 开 `workspace-task-multi-assignee-api`：`task_item_assignee` + 迁移；page/Bot/日历改写
- [x] 5.3 开 `workspace-task-multi-assignee-integrate`：移出负责人后从「我负责的」消失

## 6. P4 — 分配人与「我分配的」（子 change：assigner）

- [x] 6.1 开 `workspace-task-assigner-web`：指派 UI；「我分配的」按种子展示
- [x] 6.2 开 `workspace-task-assigner-api`：持久化 assigner；ASSIGNED_BY_ME 查询真条件
- [x] 6.3 开 `workspace-task-assigner-integrate`：A 派给 B 后 A 在「我分配的」、B 在「我负责的」

## 7. P5 — 「我负责的」个人自定义分组 · 计划 B（子 change：mine-groups）

- [x] 7.1 开 `workspace-task-mine-groups-web`：默认组 + 新建组；`groupBy=PERSONAL_CUSTOM` 看板/列表
- [x] 7.2 开 `workspace-task-mine-groups-api`：私有组表 + 任务归属；删组回默认组
- [x] 7.3 开 `workspace-task-mine-groups-integrate`：两用户同任务、分组互不可见

## 8. P6 — 多清单归属（子 change：multi-list）

- [x] 8.1 开 `workspace-task-multi-list-web`：详情多清单加入/移出
- [x] 8.2 开 `workspace-task-multi-list-api`：成员表 + 迁移 `list_id`
- [ ] 8.3 开 `workspace-task-multi-list-integrate`：一任务两清单；移出不删任务

## 9. P7 — 清单内自定义组（子 change：list-groups）

- [ ] 9.1 开 `workspace-task-list-groups-web`：默认组 + 新建分组；无字段分组时按 C 呈现
- [ ] 9.2 开 `workspace-task-list-groups-api`：`task_list_group` + 成员 `group_id`
- [ ] 9.3 开 `workspace-task-list-groups-integrate`：与字段分组共存按 D7 验收

## 10. P8 —（可选）清单自定义字段作分组源

- [ ] 10.1 关闭 design Open Question #3（存储模型）后立项
- [ ] 10.2 单选字段 CRUD + `groupBy` 指向自定义字段
- [ ] 10.3 三切片交付并更新对接看板

## 11. 文档与收尾

- [ ] 11.1 更新 `docs/dev/workspace-ui-patterns.md`：三类左栏、三套分组、快速访问种子、多清单详情
- [ ] 11.2 各子 change archive 后同步 `openspec/specs/task/spec.md`
- [ ] 11.3 P0–P7 完成后 `openspec archive workspace-task-view-model-v1`（或分阶段 sync）
