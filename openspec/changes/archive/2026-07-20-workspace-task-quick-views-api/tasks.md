## 1. 契约与迁移

- [x] 1.1 Flyway `assigner_id` + 索引
- [x] 1.2 更新 `openspec/lanes/workspace-task-quick-views/contract.md` 状态为 api ready
- [x] 1.3 看板 api → archived（或 ready）

## 2. 后端实现

- [x] 2.1 DO / Mapper.xml / RespVO 增加 `assignerId`
- [x] 2.2 `TaskItemExtMapper`：`scope=ALL` 可见并集分页
- [x] 2.3 `pageMyTasks` 支持 `ALL` / `ASSIGNED_BY_ME`
- [x] 2.4 `assign` 写入/清空 `assigner_id`
- [x] 2.5 单测覆盖 ALL/ASSIGNED_BY_ME/assigner；`./mvnw -pl relayflow-module-task/relayflow-module-task-biz -am test` 相关通过
- [x] 2.6 `./mvnw -pl relayflow-server -am compile` 通过

## 3. 收尾

- [x] 3.1 母 change `workspace-task-view-model-v1` §2.2 勾选
