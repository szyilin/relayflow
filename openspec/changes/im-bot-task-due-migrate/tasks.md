# Tasks：im-bot-task-due-migrate

> 验证：`openspec validate im-bot-task-due-migrate --strict`；`./mvnw -pl relayflow-module-task/relayflow-module-task-biz -am test -Dtest=TaskDueNotifyServiceTest -Dsurefire.failIfNoSpecifiedTests=false`；`./mvnw -pl relayflow-server -am compile`

## 1. 依赖与产方实现

- [x] 1.1 `task-biz` pom 增加 `relayflow-module-im-api`（仅 `*-api`）
- [x] 1.2 `TaskDueNotifyService`：窗口内 `TODO` → `ImBotApi.send(task-bot, SINGLE)`；`dedupeKey=TASK_DUE:{taskId}`；文案 + `route`/`entityType`/`entityId`；send 失败 catch 记日志不抛
- [x] 1.3 若出现 Bean 环，对 `ImBotApi` 使用 `@Lazy`（与 invite 产方同模式）— 本切片无环，未加 `@Lazy`

## 2. 测试与收口

- [x] 2.1 扩展 `TaskDueNotifyServiceTest`：校验 send 参数；窗口外不调用；send 抛错不向外抛
- [x] 2.2 `openspec validate im-bot-task-due-migrate --strict`；相关单测 + `./mvnw -pl relayflow-server -am compile` 通过
- [x] 2.3 勾选母 change `im-bot-notify-foundation` §7.2；更新 `docs/dev/api-integration-board.md` 产方迁移状态
