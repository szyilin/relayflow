## 1. Contract

- [x] 1.1 更新 `openspec/lanes/workspace-task-list/contract.md`：`GET /list/archived`、`POST /list/unarchive`、错误码与前端行为
- [x] 1.2 更新 `docs/dev/api-integration-board.md` 登记本切片

## 2. API

- [x] 2.1 `TaskListService` + Controller：`listArchived`、`unarchive`（OWNER；幂等）
- [x] 2.2 单元/编译验证：`./mvnw -pl relayflow-server -am compile`

## 3. Web

- [x] 3.1 `api/app/taskList.ts` + `tasks` store：`fetchArchivedLists`、`unarchiveList`
- [x] 3.2 侧栏「已归档」折叠区 + 恢复；归档确认弹窗
- [x] 3.3 `pnpm build` + `pnpm typecheck`

## 4. Integrate

- [x] 4.1 联调路径说明：归档 → 已归档可见 → 恢复 → 重回侧栏
- [x] 4.2 勾选完成；`openspec validate workspace-task-list-archive-restore --strict`（若 CLI 未知 item 则以 change artifacts + 编译/前端验证为准）
