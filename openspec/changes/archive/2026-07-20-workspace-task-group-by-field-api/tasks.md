## 1. 契约与错误码

- [x] 1.1 错误码 `TASK_GROUP_MOVE_INVALID`
- [x] 1.2 冻结 `openspec/lanes/workspace-task-group-by-field/contract.md`（api ready）；看板
- [x] 1.3 文档注明 `board-move` 过渡兼容

## 2. 实现

- [x] 2.1 `TaskItemGroupMoveReqVO` + Controller
- [x] 2.2 Service `groupMove`（status/dueTime/assigneeId）
- [x] 2.3 单测；`./mvnw -pl relayflow-module-task/relayflow-module-task-biz -am test -Dtest=TaskItemServiceImplTest`

## 3. 收尾

- [x] 3.1 母 change §4.2 勾选
