## 1. 契约与迁移

- [x] 1.1 Flyway `V0.1.0.29__task_item_assignee.sql`（建表 + 回填）
- [x] 1.2 DO/Mapper（对齐 follower 模式）
- [x] 1.3 冻结 contract api ready；看板

## 2. 实现

- [x] 2.1 `TaskAssigneeService.replaceAssignees` + 投影 `assignee_id`
- [x] 2.2 Controller `PUT /assignees`；`assign` 委托
- [x] 2.3 page/search/due/union/access 读集合；响应填 `assigneeIds`
- [x] 2.4 due Bot fan-out；单测 + compile

## 3. 收尾

- [x] 3.1 母 change §5.2 勾选
