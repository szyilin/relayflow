## 1. 契约与看板

- [x] 1.1 起草 `openspec/lanes/workspace-task-quick-views/contract.md`（contextType、默认筛选种子、page API 扩展草案）
- [x] 1.2 更新 `docs/dev/api-integration-board.md`：本切片 web=in_progress

## 2. 前端导航与 Store

- [x] 2.1 扩展 `TasksNavView`：`all` | `assigned_by_me`；路由 `?view=` 解析
- [x] 2.2 左栏重组：个人入口 / 快速访问 / 清单；标题与空状态文案
- [x] 2.3 Store：各快捷视图默认种子；`ALL`/`ASSIGNED_BY_ME` store 内临时数据（可关 flag）
- [x] 2.4 `pnpm build` + `pnpm typecheck` 通过

## 3. 收尾

- [x] 3.1 母 change `workspace-task-view-model-v1` tasks §2.1 勾选
- [x] 3.2 看板 web → ui_ready；写明浏览器验证路径
